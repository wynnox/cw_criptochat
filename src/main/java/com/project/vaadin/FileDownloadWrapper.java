package com.project.vaadin;

import com.vaadin.flow.component.Component;
import com.vaadin.flow.component.Composite;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.server.StreamResource;

public class FileDownloadWrapper extends Composite<Anchor> {
  public FileDownloadWrapper(StreamResource resource) {
    getContent().setHref(resource);
    getContent().getElement().setAttribute("download", true);
  }

  public void wrapComponent(Component component) {
    getContent().removeAll();
    getContent().add(component);
  }
}
