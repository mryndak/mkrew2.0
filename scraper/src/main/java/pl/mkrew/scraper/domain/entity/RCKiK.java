package pl.mkrew.scraper.domain.entity;

import jakarta.persistence.*;

@Entity
@Table(name = "rckik")
public class RCKiK {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "code", nullable = false, unique = true, length = 50)
    private String code;

    @Column(name = "name", nullable = false)
    private String name;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "website_url", length = 500)
    private String websiteUrl;

    @Column(name = "scraping_enabled", nullable = false)
    private Boolean scrapingEnabled = true;

    @Column(name = "scraping_cron_expression", length = 100)
    private String scrapingCronExpression = "0 0 8,14,20 * * ?";

    public RCKiK() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = code;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCity() {
        return city;
    }

    public void setCity(String city) {
        this.city = city;
    }

    public String getWebsiteUrl() {
        return websiteUrl;
    }

    public void setWebsiteUrl(String websiteUrl) {
        this.websiteUrl = websiteUrl;
    }

    public Boolean getScrapingEnabled() {
        return scrapingEnabled;
    }

    public void setScrapingEnabled(Boolean scrapingEnabled) {
        this.scrapingEnabled = scrapingEnabled;
    }

    public String getScrapingCronExpression() {
        return scrapingCronExpression;
    }

    public void setScrapingCronExpression(String scrapingCronExpression) {
        this.scrapingCronExpression = scrapingCronExpression;
    }
}
