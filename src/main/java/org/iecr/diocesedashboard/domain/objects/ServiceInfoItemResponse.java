package org.iecr.diocesedashboard.domain.objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;

@Entity
public class ServiceInfoItemResponse {

  @Id
  @GeneratedValue(strategy = GenerationType.AUTO)
  @Column(unique = true, nullable = false)
  private Long id;

  @ManyToOne
  @JoinColumn(name = "service_info_item_id", nullable = false)
  private ServiceInfoItem serviceInfoItem;

  @ManyToOne
  @JoinColumn(name = "service_instance_id", nullable = false)
  private ServiceInstance serviceInstance;

  @Column
  private String responseValue;

  public ServiceInfoItemResponse() {
  }

  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ServiceInfoItem getServiceInfoItem() {
    return serviceInfoItem;
  }

  public void setServiceInfoItem(ServiceInfoItem serviceInfoItem) {
    this.serviceInfoItem = serviceInfoItem;
  }

  public ServiceInstance getServiceInstance() {
    return serviceInstance;
  }

  public void setServiceInstance(ServiceInstance serviceInstance) {
    this.serviceInstance = serviceInstance;
  }

  public String getResponseValue() {
    return responseValue;
  }

  public void setResponseValue(String responseValue) {
    this.responseValue = responseValue;
  }

  @Override
  public String toString() {
    return "ServiceInfoItemResponse{id=" + id + ", responseValue='" + responseValue + "'}";
  }
}
