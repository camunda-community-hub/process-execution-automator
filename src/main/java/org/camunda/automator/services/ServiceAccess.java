package org.camunda.automator.services;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * This class reference all services, and can be pass in any new object to give access to all services
 */
@Service
public class ServiceAccess {

  @Autowired
  public ServiceDataOperation serviceDataOperation;
}
