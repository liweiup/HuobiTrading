//package com.contract.harvest.config;
//
//import com.zaxxer.hikari.HikariConfig;
//import com.zaxxer.hikari.HikariDataSource;
//import org.apache.ibatis.session.SqlSessionFactory;
//import org.mybatis.spring.SqlSessionFactoryBean;
//import org.springframework.beans.factory.annotation.Qualifier;
//import org.springframework.beans.factory.annotation.Value;
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.context.annotation.Primary;
//
//import javax.sql.DataSource;
//
////@Configuration
//public class MysqlConfig {
//
//    @Value("${spring.datasource.driver-class-name}")
//    private String driverClassName;
//
//    @Value("${spring.datasource.mysql.jdbc-url}")
//    private String jdbcUrl;
//
//    @Value("${spring.datasource.mysql.username}")
//    private String userName;
//
//    @Value("${spring.datasource.mysql.password}")
//    private String password;
//
//    @Value("${spring.datasource.hikari.max-lifetime}")
//    private long maxLifetimeMs;
//
//    @Value("${spring.datasource.hikari.idle-timeout}")
//    private long idleTimeoutMs;
//
//    @Value("${spring.datasource.hikari.maximum-pool-size}")
//    private int maxPoolSize;
//
//    @Value("${spring.datasource.hikari.minimum-idle}")
//    private int minIdle;
//
//    @Primary
//    @Bean(name = "mySQLDataSource")
//    public DataSource mysqlDataSource() {
//        HikariConfig config = new HikariConfig();
//        config.setDriverClassName(driverClassName);
//        config.setJdbcUrl(jdbcUrl);
//        config.setUsername(userName);
//        config.setPassword(password);
//        config.setMaxLifetime(maxLifetimeMs);
//        config.setIdleTimeout(idleTimeoutMs);
//        config.setMaximumPoolSize(maxPoolSize);
//        config.setMinimumIdle(minIdle);
//        return new HikariDataSource(config);
//    }
//
//}
