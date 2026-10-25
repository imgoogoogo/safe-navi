package com.safenavi.backend.sync;

import java.util.List;
import java.util.Map;

/** One fetched page, with each item flattened to column name -> text value. */
public record PublicDataPage(List<Map<String, String>> items, int totalCount) {
}
