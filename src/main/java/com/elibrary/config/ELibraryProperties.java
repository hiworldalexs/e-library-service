package com.elibrary.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "e-library-service")
public class ELibraryProperties {

    private int maxConcurrentBorrows;

    private int loanPeriodDays;
}
