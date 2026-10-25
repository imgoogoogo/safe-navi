package com.safenavi.backend.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class AddressGeocoderTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void parsesPoint() {
        String body = """
                {"meta":{"total_count":1,"pageable_count":1,"is_end":true},
                 "documents":[{"address_name":"경북 경주시 원화로 266","address_type":"ROAD_ADDR",
                  "x":"129.21752042922358","y":"35.84484721643797"}]}
                """;

        assertThat(AddressGeocoder.parse(mapper.readTree(body))).hasValueSatisfying(c -> {
            assertThat(c.lat()).isEqualTo(35.84484721643797);
            assertThat(c.lng()).isEqualTo(129.21752042922358);
        });
    }

    @Test
    void cleansAddressesTheGeocoderRejects() {
        assertThat(AddressGeocoder.clean("경상북도 경주시 문무대왕면 어일1길 39(어일리 837-2)"))
                .isEqualTo("경상북도 경주시 문무대왕면 어일1길 39");
        assertThat(AddressGeocoder.clean("서울특별시 관악구 난곡로64 난우파출소")).isEqualTo("서울특별시 관악구 난곡로 64");
        assertThat(AddressGeocoder.clean("부산광역시 동구 초량상로 65번길2")).isEqualTo("부산광역시 동구 초량상로 65번길 2");
        assertThat(AddressGeocoder.clean("서울특별시 동대문구 약령시로21가길 15")).isEqualTo("서울특별시 동대문구 약령시로21가길 15");
    }

    @Test
    void noDocumentsIsEmpty() {
        String body = "{\"meta\":{\"total_count\":0,\"pageable_count\":0,\"is_end\":true},\"documents\":[]}";

        assertThat(AddressGeocoder.parse(mapper.readTree(body))).isEmpty();
    }
}
