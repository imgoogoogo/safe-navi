package com.safenavi.backend.sync;

import java.io.StringReader;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.util.UriComponentsBuilder;
import org.w3c.dom.Element;

import com.safenavi.backend.sync.PublicDataProperties.Source;
import com.safenavi.backend.sync.xml.XmlResponse;

import jakarta.xml.bind.JAXBContext;
import jakarta.xml.bind.JAXBException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

@Component
public class PublicDataClient {

    /** Standard-data APIs answer "00"; newer apis.data.go.kr services (e.g. MOIS CCTV) answer "0". */
    private static final Set<String> JSON_OK_CODES = Set.of("00", "0");
    private static final String OK_CODE = "00";

    private final PublicDataProperties properties;
    private final JsonMapper jsonMapper;
    private final JAXBContext jaxbContext;
    private final WebClient webClient;

    public PublicDataClient(PublicDataProperties properties, JsonMapper jsonMapper) throws JAXBException {
        this.properties = properties;
        this.jsonMapper = jsonMapper;
        this.jaxbContext = JAXBContext.newInstance(XmlResponse.class);
        this.webClient = WebClient.builder()
                .codecs(c -> c.defaultCodecs().maxInMemorySize(32 * 1024 * 1024))
                .build();
    }

    public PublicDataPage fetchPage(Source source, int pageNo) {
        String body = webClient.get()
                .uri(buildUri(source, pageNo))
                .retrieve()
                .bodyToMono(String.class)
                .block(properties.timeout());
        if (body == null || body.isBlank()) {
            throw new PublicDataException(source.name() + ": empty response");
        }
        if (source.isOdcloud()) {
            return parseOdcloud(source, body);
        }
        return source.isXml() ? parseXml(source, body) : parseJson(source, body);
    }

    /**
     * serviceKey is expanded as a URI variable after encoding the template, so the '+', '/' and '='
     * in data.go.kr keys are percent-encoded instead of being mangled.
     */
    private URI buildUri(Source source, int pageNo) {
        UriComponentsBuilder builder = UriComponentsBuilder.fromUriString(source.url())
                .queryParam("serviceKey", "{serviceKey}");
        if (source.isOdcloud()) {
            builder.queryParam("page", pageNo).queryParam("perPage", properties.pageSize());
        } else {
            builder.queryParam("pageNo", pageNo).queryParam("numOfRows", properties.pageSize());
            if (!source.isXml()) {
                builder.queryParam("type", "json");
            }
        }
        return builder.encode().buildAndExpand(Map.of("serviceKey", properties.serviceKey())).toUri();
    }

    PublicDataPage parseJson(Source source, String body) {
        if (body.stripLeading().startsWith("<")) {
            // data.go.kr reports gateway errors (bad key, quota) as XML even for JSON requests
            throw new PublicDataException(source.name() + ": API error " + abbreviate(body));
        }
        try {
            // Some APIs wrap header/body in "response", others return them at the root
            JsonNode root = jsonMapper.readTree(body);
            JsonNode response = root.has("response") ? root.get("response") : root;
            String code = response.path("header").path("resultCode").asString("");
            if (!JSON_OK_CODES.contains(code)) {
                throw new PublicDataException(source.name() + ": resultCode=" + code + " "
                        + response.path("header").path("resultMsg").asString(""));
            }
            JsonNode bodyNode = response.path("body");
            JsonNode items = bodyNode.path("items");
            if (items.isObject() && items.has("item")) {
                items = items.get("item");
            }
            List<Map<String, String>> rows = new ArrayList<>();
            if (items.isArray()) {
                items.forEach(item -> rows.add(flatten(item)));
            } else if (items.isObject()) {
                rows.add(flatten(items));
            }
            return new PublicDataPage(rows, bodyNode.path("totalCount").asInt(0));
        } catch (JacksonException e) {
            throw new PublicDataException(source.name() + ": invalid JSON " + abbreviate(body), e);
        }
    }

    /** api.odcloud.kr has no result header: errors are HTTP statuses, rows sit under "data". */
    PublicDataPage parseOdcloud(Source source, String body) {
        try {
            JsonNode root = jsonMapper.readTree(body);
            JsonNode data = root.path("data");
            if (!data.isArray()) {
                throw new PublicDataException(source.name() + ": unexpected response " + abbreviate(body));
            }
            List<Map<String, String>> rows = new ArrayList<>();
            data.forEach(item -> rows.add(flatten(item)));
            // matchCount is the size of the (possibly filtered) result set being paged
            return new PublicDataPage(rows, root.path("matchCount").asInt(root.path("totalCount").asInt(0)));
        } catch (JacksonException e) {
            throw new PublicDataException(source.name() + ": invalid JSON " + abbreviate(body), e);
        }
    }

    PublicDataPage parseXml(Source source, String body) {
        XmlResponse response;
        try {
            response = (XmlResponse) jaxbContext.createUnmarshaller().unmarshal(new StringReader(body));
        } catch (JAXBException e) {
            throw new PublicDataException(source.name() + ": unexpected XML " + abbreviate(body), e);
        }
        String code = response.header == null ? null : response.header.resultCode;
        if (!OK_CODE.equals(code)) {
            throw new PublicDataException(source.name() + ": resultCode=" + code + " "
                    + (response.header == null ? "" : response.header.resultMsg));
        }
        List<Map<String, String>> rows = new ArrayList<>();
        if (response.body != null && response.body.items != null) {
            for (XmlResponse.Item item : response.body.items.item) {
                Map<String, String> row = new HashMap<>();
                for (Element field : item.fields) {
                    row.put(field.getLocalName() != null ? field.getLocalName() : field.getTagName(),
                            field.getTextContent());
                }
                rows.add(row);
            }
        }
        int total = response.body == null || response.body.totalCount == null ? 0 : response.body.totalCount;
        return new PublicDataPage(rows, total);
    }

    private static Map<String, String> flatten(JsonNode item) {
        Map<String, String> row = new HashMap<>();
        item.properties().forEach(e -> {
            JsonNode v = e.getValue();
            if (!v.isNull()) {
                row.put(e.getKey(), v.isValueNode() ? v.asString() : v.toString());
            }
        });
        return row;
    }

    private static String abbreviate(String s) {
        String flat = s.replaceAll("\\s+", " ").trim();
        return flat.length() > 200 ? flat.substring(0, 200) + "…" : flat;
    }
}
