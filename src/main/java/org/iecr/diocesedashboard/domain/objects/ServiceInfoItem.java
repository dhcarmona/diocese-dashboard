package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import java.util.Set;

@Entity
public class ServiceInfoItem {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(unique = true, nullable = false)
  private Long id;

  @Column(unique = true, nullable = false)
  private String questionId;

  @ManyToOne
  @JoinColumn(nullable = false)
  private ServiceTemplate serviceTemplate;

  private Boolean required;

  @Column(nullable = false)
  private ServiceInfoItemType serviceInfoItemType;

  @OneToMany(mappedBy = "serviceInfoItem")
  private Set<ServiceInfoItemResponse> responses;

  public ServiceInfoItem() {}

  public Long getId() { return id; }
  public void setId(Long id) { this.id = id; }

  public String getQuestionId() { return questionId; }
  public void setQuestionId(String questionId) { this.questionId = questionId; }

  public ServiceTemplate getServiceTemplate() { return serviceTemplate; }
  public void setServiceTemplate(ServiceTemplate serviceTemplate) { this.serviceTemplate = serviceTemplate; }

  public Boolean getRequired() { return required; }
  public void setRequired(Boolean required) { this.required = required; }

  public ServiceInfoItemType getServiceInfoItemType() { return serviceInfoItemType; }
  public void setServiceInfoItemType(ServiceInfoItemType serviceInfoItemType) { this.serviceInfoItemType = serviceInfoItemType; }

  public Set<ServiceInfoItemResponse> getResponses() { return responses; }
  public void setResponses(Set<ServiceInfoItemResponse> responses) { this.responses = responses; }

  @Override
  public String toString() {
    return "ServiceInfoItem{id=" + id + ", questionId='" + questionId + "'}";
  }
}

