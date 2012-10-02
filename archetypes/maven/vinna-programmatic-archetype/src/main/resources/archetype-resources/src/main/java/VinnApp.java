package ${package};

import java.util.Map;

import ${package}.controllers.HelloControler;

import vinna.Vinna;

public class VinnApp extends Vinna {

  @Override
  protected void routes(Map<String, Object> config) {
    get("/hello/{name}").withController(HelloController.class).sayHello(param("name").asString());
  }
}