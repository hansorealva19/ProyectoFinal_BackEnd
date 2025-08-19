package order.config;

import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class StartupInfo {

    private static final Logger log = LoggerFactory.getLogger(StartupInfo.class);

    @Autowired
    private Environment env;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void info() {
        try {
            String url = env.getProperty("spring.datasource.url");
            log.info("[StartupInfo] spring.datasource.url={}", url);
            // Fallback prints for consoles that don't show SLF4J
            System.out.println("[StartupInfo] spring.datasource.url=" + url);
            String schema = jdbcTemplate.queryForObject("SELECT DATABASE()", String.class);
            log.info("[StartupInfo] connected to database: {}", schema);
            System.out.println("[StartupInfo] connected to database: " + schema);
        } catch (Exception e) {
            log.error("[StartupInfo] Could not read datasource info: {}", e.getMessage(), e);
            System.err.println("[StartupInfo] Could not read datasource info: " + e.getMessage());
        }
    }
}
