package com.safenavi.backend.geo;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import tools.jackson.databind.json.JsonMapper;

class GeoServiceTest {

    private final JsonMapper mapper = JsonMapper.builder().build();

    @Test
    void mapsNominatimKoreanAddress() {
        var address = mapper.readTree("""
                {"road":"테헤란로","house_number":"152","quarter":"역삼1동","suburb":"역삼동",
                 "borough":"강남구","city":"서울특별시","postcode":"06236","country":"대한민국"}
                """);

        ReverseGeocodeResponse result = GeoService.map(address);

        assertThat(result.dong()).isEqualTo("역삼1동");
        assertThat(result.roadAddress()).isEqualTo("서울특별시 강남구 테헤란로 152");
    }

    @Test
    void noRoadMeansNoRoadAddress() {
        ReverseGeocodeResponse result = GeoService.map(mapper.readTree("{\"suburb\":\"역삼동\"}"));

        assertThat(result.dong()).isEqualTo("역삼동");
        assertThat(result.roadAddress()).isNull();
    }
}
