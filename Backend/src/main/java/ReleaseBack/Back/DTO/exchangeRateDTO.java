package ReleaseBack.Back.DTO;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class exchangeRateDTO {
    @JsonProperty("base_currency")
    @JsonAlias("base")
    private String base;

    @JsonProperty("base_currency_name")
    private String baseCurrencyName;

    @JsonProperty("quote_currency")
    @JsonAlias("quote")
    private String quote;

    @JsonProperty("quote_currency_name")
    private String quoteCurrencyName;

    private Double rate;
    private String date;
    private String source;
}
