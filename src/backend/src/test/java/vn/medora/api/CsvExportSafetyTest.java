package vn.medora.api;

import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import vn.medora.api.governance.ReportService;
import static org.assertj.core.api.Assertions.assertThat;

class CsvExportSafetyTest {
    private final ReportService reports = new ReportService(null, null, null);

    @Test void spreadsheetFormulaPrefixesAreNeutralizedAndQuoted() {
        for (String hostile : List.of("=1+1", "+SUM(A1)", "-1+2", "@SUM(A1)", "\t=1", "\r=1")) {
            String csv = reports.csv(List.of(Map.of("value", hostile)), List.of("value"));
            assertThat(csv).isEqualTo("value\r\n\"'" + hostile + "\"\r\n");
        }
    }
    @Test void quotesCommasAndNewlinesCannotEscapeTheExportCell() {
        String csv = reports.csv(List.of(Map.of("description", "UAT, \"quoted\"\nnext row", "amount", 0)),
            List.of("description", "amount"));
        assertThat(csv).isEqualTo("description,amount\r\n\"UAT, \"\"quoted\"\"\nnext row\",\"0\"\r\n");
    }
}
