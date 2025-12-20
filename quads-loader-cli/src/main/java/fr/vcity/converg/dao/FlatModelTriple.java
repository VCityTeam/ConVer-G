package fr.vcity.converg.dao;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Column;
import org.springframework.data.relational.core.mapping.Table;

@Data
@Table(name = "flat_model_triple")
public class FlatModelTriple {
    @Id
    private Integer id;

    @Column(value = "subject")
    private String subject;

    @Column(value = "subject_type")
    private String subjectType;

    @Column(value = "predicate")
    private String predicate;

    @Column(value = "predicate_type")
    private String predicateType;

    @Column(value = "object")
    private String object;

    @Column(value = "object_type")
    private String objectType;
}