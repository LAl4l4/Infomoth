package ReleaseBack.Back.DTO;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class SettingsDTO {
    private Integer defaultPage;
    private String defaultBaseCurrency;
    private String defaultQuoteCurrency;

    public SettingsDTO(Integer defaultPage) {
        this.defaultPage = defaultPage;
    }

    public SettingsDTO(Integer defaultPage, String defaultBaseCurrency, String defaultQuoteCurrency) {
        this.defaultPage = defaultPage;
        this.defaultBaseCurrency = defaultBaseCurrency;
        this.defaultQuoteCurrency = defaultQuoteCurrency;
    }
}
