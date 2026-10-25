package com.safenavi.backend.sync;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.safenavi.backend.sync.PublicDataProperties.Source;

import tools.jackson.databind.json.JsonMapper;

class PublicDataClientTest {

    private final Source json = new Source("cctv-standard", "cctv", "json", "https://example.test", null,
            "latitude", "longitude", null, true);
    private final Source odcloud = new Source("police", "police", "odcloud", "https://example.test", "주소",
            null, null, "주소", true);
    private final Source xml = new Source("emergency-medical", "emergencyMedical", "xml", "https://example.test",
            "hpid", "wgs84Lat", "wgs84Lon", null, true);
    private final PublicDataClient client;

    PublicDataClientTest() throws Exception {
        client = new PublicDataClient(new PublicDataProperties("key", "0 0 4 * * *", 100, Duration.ofSeconds(5), List.of()),
                JsonMapper.builder().build());
    }

    @Test
    void parsesStandardDataJson() {
        String body = """
                {"response":{"header":{"resultCode":"00","resultMsg":"NORMAL_SERVICE"},
                 "body":{"items":[{"latitude":"37.5","longitude":"127.0","cameraCount":"2"}],
                 "totalCount":"1","numOfRows":"100","pageNo":"1"}}}
                """;

        PublicDataPage page = client.parseJson(json, body);

        assertThat(page.totalCount()).isEqualTo(1);
        assertThat(page.items()).singleElement()
                .satisfies(item -> assertThat(item).containsEntry("latitude", "37.5").containsEntry("cameraCount", "2"));
    }

    @Test
    void parsesJsonWithoutResponseWrapper() {
        String body = """
                {"header":{"resultCode":"00","resultMsg":"NORMAL SERVICE."},
                 "body":{"items":{"item":[{"latitude":"37.4","longitude":"126.6"}]},
                 "numOfRows":2,"pageNo":1,"totalCount":2852}}
                """;

        PublicDataPage page = client.parseJson(json, body);

        assertThat(page.totalCount()).isEqualTo(2852);
        assertThat(page.items()).singleElement()
                .satisfies(item -> assertThat(item).containsEntry("latitude", "37.4"));
    }

    @Test
    void acceptsSingleZeroResultCode() {
        String body = """
                {"response":{"body":{"dataType":"JSON","items":{"item":[{"MNG_NO":"1","WGS84_LAT":"35.1",
                 "WGS84_LOT":"128.8","KPNG_DAY_CNT":null}]},"numOfRows":100,"pageNo":1,"totalCount":381409},
                 "header":{"resultCode":"0","resultMsg":"정상"}}}
                """;

        PublicDataPage page = client.parseJson(json, body);

        assertThat(page.totalCount()).isEqualTo(381409);
        assertThat(page.items()).singleElement()
                .satisfies(item -> assertThat(item).containsEntry("MNG_NO", "1").doesNotContainKey("KPNG_DAY_CNT"));
    }

    @Test
    void jsonErrorResultCodeIsReported() {
        String body = """
                {"header":{"resultCode":"10","resultMsg":"INVALID_REQUEST_PARAMETER_ERROR (numOfRows>1000)"},"body":null}
                """;

        assertThatThrownBy(() -> client.parseJson(json, body))
                .isInstanceOf(PublicDataException.class)
                .hasMessageContaining("resultCode=10");
    }

    @Test
    void parsesOdcloudRows() {
        String body = """
                {"currentCount":1,"data":[{"경찰서":"경주","관서명":"역전","구분":"파출소","시도청":"경북청","연번":1627,
                 "주소":"경상북도 경주시 원화로 270"}],"matchCount":22,"page":1,"perPage":1,"totalCount":2047}
                """;

        PublicDataPage page = client.parseOdcloud(odcloud, body);

        assertThat(page.totalCount()).isEqualTo(22);
        assertThat(page.items()).singleElement()
                .satisfies(item -> assertThat(item).containsEntry("주소", "경상북도 경주시 원화로 270")
                        .containsEntry("연번", "1627"));
    }

    @Test
    void odcloudErrorBodyIsReported() {
        assertThatThrownBy(() -> client.parseOdcloud(odcloud, "{\"code\":-401,\"msg\":\"유효하지 않은 인증키 입니다.\"}"))
                .isInstanceOf(PublicDataException.class)
                .hasMessageContaining("unexpected response");
    }

    @Test
    void jsonGatewayErrorInXmlIsReported() {
        String body = "<OpenAPI_ServiceResponse><cmmMsgHeader><returnAuthMsg>SERVICE_KEY_IS_NOT_REGISTERED_ERROR"
                + "</returnAuthMsg></cmmMsgHeader></OpenAPI_ServiceResponse>";

        assertThatThrownBy(() -> client.parseJson(json, body))
                .isInstanceOf(PublicDataException.class)
                .hasMessageContaining("SERVICE_KEY_IS_NOT_REGISTERED_ERROR");
    }

    @Test
    void parsesDataGoKrXmlWithJaxb() {
        String body = """
                <?xml version="1.0" encoding="UTF-8"?>
                <response><header><resultCode>00</resultCode><resultMsg>NORMAL SERVICE.</resultMsg></header>
                <body><items>
                  <item><hpid>A1100001</hpid><dutyName>테스트병원</dutyName><wgs84Lat>37.51</wgs84Lat><wgs84Lon>127.02</wgs84Lon></item>
                  <item><hpid>A1100002</hpid><wgs84Lat>37.52</wgs84Lat><wgs84Lon>127.03</wgs84Lon></item>
                </items><numOfRows>10</numOfRows><pageNo>1</pageNo><totalCount>2</totalCount></body></response>
                """;

        PublicDataPage page = client.parseXml(xml, body);

        assertThat(page.totalCount()).isEqualTo(2);
        assertThat(page.items()).hasSize(2);
        assertThat(page.items().get(0)).containsEntry("hpid", "A1100001").containsEntry("dutyName", "테스트병원")
                .containsEntry("wgs84Lat", "37.51");
    }

    @Test
    void nonOkResultCodeFails() {
        String body = "<response><header><resultCode>30</resultCode><resultMsg>SERVICE KEY IS NOT REGISTERED</resultMsg>"
                + "</header></response>";

        assertThatThrownBy(() -> client.parseXml(xml, body)).hasMessageContaining("resultCode=30");
    }
}
