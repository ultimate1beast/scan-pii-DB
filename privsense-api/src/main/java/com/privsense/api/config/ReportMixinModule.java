package com.privsense.api.config;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.Module;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.privsense.core.model.*;
import org.springframework.stereotype.Component;

/**
 * Jackson module for configuring special serialization handling for 
 * compliance report-related classes to avoid circular reference issues.
 */
@Component
public class ReportMixinModule extends SimpleModule {
    
    private static final long serialVersionUID = 1L;
    
    public ReportMixinModule() {
        super("ReportMixinModule", new Version(1, 0, 0, null, null, null));
        // Set up any additional specialized serializers or configurations here
        setMixInAnnotation(TableInfo.class, TableInfoMixin.class);
        setMixInAnnotation(ColumnInfo.class, ColumnInfoMixin.class);
        setMixInAnnotation(RelationshipInfo.class, RelationshipInfoMixin.class);
        setMixInAnnotation(ComplianceReport.class, ComplianceReportMixin.class);
    }
    
    /**
     * Register this module with the ObjectMapper
     */
    @Override
    public void setupModule(Module.SetupContext context) {
        super.setupModule(context);
    }
    
    /**
     * Mixin abstract class for TableInfo to control serialization
     */
    public abstract static class TableInfoMixin {
        // Leave empty - this is just a marker class for Jackson
    }
    
    /**
     * Mixin abstract class for ColumnInfo to control serialization
     */
    public abstract static class ColumnInfoMixin {
        // Leave empty - this is just a marker class for Jackson
    }
    
    /**
     * Mixin abstract class for RelationshipInfo to control serialization
     */
    public abstract static class RelationshipInfoMixin {
        // Leave empty - this is just a marker class for Jackson
    }
    
    /**
     * Mixin abstract class for ComplianceReport to control serialization
     */
    public abstract static class ComplianceReportMixin {
        // Leave empty - this is just a marker class for Jackson
    }
}