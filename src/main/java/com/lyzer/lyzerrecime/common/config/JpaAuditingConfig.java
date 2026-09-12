package com.lyzer.lyzerrecime.common.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

/**
 * Activates @CreatedDate / @LastModifiedDate population.
 *
 * Deliberately its own @Configuration rather than an annotation on the
 * application class: @WebMvcTest loads the application class but excludes JPA
 * autoconfiguration, so the auditing handler would fail to resolve
 * jpaMappingContext and every slice test would die on context load. A plain
 * @Configuration is filtered out by WebMvcTypeExcludeFilter instead.
 */
@Configuration
@EnableJpaAuditing
public class JpaAuditingConfig {
}
