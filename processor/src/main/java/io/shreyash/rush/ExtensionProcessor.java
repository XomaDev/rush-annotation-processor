package io.shreyash.rush;

import com.amihaiemil.eoyaml.exceptions.YamlReadingException;
import com.google.appinventor.components.annotations.SimpleEvent;
import com.google.appinventor.components.annotations.SimpleFunction;
import com.google.auto.service.AutoService;
import io.shreyash.rush.blocks.*;
import io.shreyash.rush.util.InfoFilesGenerator;
import org.xml.sax.SAXException;

import javax.annotation.processing.*;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.Modifier;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Set;

@AutoService(Processor.class)
@SupportedSourceVersion(SourceVersion.RELEASE_8)
@SupportedAnnotationTypes("com.google.appinventor.components.annotations.*")
public class ExtensionProcessor extends AbstractProcessor {

  private ExtensionFieldInfo extensionFieldInfo;
  private boolean isFirstRound = true;

  @Override
  public synchronized void init(ProcessingEnvironment processingEnv) {
    super.init(processingEnv);
    this.extensionFieldInfo = new ExtensionFieldInfo();
  }

  @Override
  public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
    if (!isFirstRound) {
      return true;
    }
    isFirstRound = false;

    final Messager messager = processingEnv.getMessager();
    final String org = processingEnv.getOptions().get("org");
    final String extName = processingEnv.getOptions().get("extName");

    // Process all SimpleEvents
    for (Element el : roundEnv.getElementsAnnotatedWith(SimpleEvent.class)) {
      if (!el.getEnclosingElement().getSimpleName().toString().equals(extName)) {
        messager.printMessage(Diagnostic.Kind.ERROR, "ERR Annotation @SimpleEvent can't be used on element '" + el.getSimpleName() + "'. It can only be used on members of class '" + org + "." + extName + "'.");
        continue;
      }
      if (!el.getModifiers().contains(Modifier.PRIVATE)) {
        Event event = new Event(el, messager).build();
        extensionFieldInfo.addEvent(event);
      } else {
        messager.printMessage(Diagnostic.Kind.ERROR, "ERR Private element '" + el.getSimpleName() + "' can't be annotated with @SimpleEvent.");
      }
    }

    // Process all SimpleFunctions
    for (Element el : roundEnv.getElementsAnnotatedWith(SimpleFunction.class)) {
      if (!el.getEnclosingElement().getSimpleName().toString().equals(extName)) {
        messager.printMessage(Diagnostic.Kind.ERROR, "ERR Annotation @SimpleFunction can't be used on element '" + el.getSimpleName() + "'. It can only be used on members of class '" + org + "." + extName + "'.");
        continue;
      }
      if (!el.getModifiers().contains(Modifier.PRIVATE)) {
        Function func = new Function(el, messager).build();
        extensionFieldInfo.addFunction(func);
      } else {
        messager.printMessage(Diagnostic.Kind.ERROR, "ERR Private element '" + el.getSimpleName() + "' can't be annotated with @SimpleFunction.");
      }
    }

    // Process all SimpleProps
    for (Element el : roundEnv.getElementsAnnotatedWith(com.google.appinventor.components.annotations.SimpleProperty.class)) {
      if (!el.getEnclosingElement().getSimpleName().toString().equals(extName)) {
        messager.printMessage(Diagnostic.Kind.ERROR, "ERR Annotation @SimpleProperty can't be used on element '" + el.getSimpleName() + "'. It can only be used on members of class '" + org + "." + extName + "'.");
        continue;
      }
      if (!el.getModifiers().contains(Modifier.PRIVATE)) {
        Property prop = new Property(el, extensionFieldInfo, messager).build();
        if (prop.getName() != null) {
          extensionFieldInfo.addBlockProp(prop);
        }
      } else {
        messager.printMessage(Diagnostic.Kind.ERROR, "ERR Private element '" + el.getSimpleName() + "' can't be annotated with @SimpleProperty.");
      }
    }

    // Process all DesignerProps
    for (Element el : roundEnv.getElementsAnnotatedWith(com.google.appinventor.components.annotations.DesignerProperty.class)) {
      if (!el.getEnclosingElement().getSimpleName().toString().equals(extName)) {
        messager.printMessage(Diagnostic.Kind.ERROR, "ERR Annotation @DesignerProperty can't be used on element '" + el.getSimpleName() + "'. It can only be used on members of class '" + org + "." + extName + "'.");
        continue;
      }
      if (!el.getModifiers().contains(Modifier.PRIVATE)) {
        DesignerProperty prop = new DesignerProperty(el, extensionFieldInfo, messager).build();
        if (prop.getName() != null) {
          extensionFieldInfo.addProp(prop);
        }
      } else {
        messager.printMessage(Diagnostic.Kind.ERROR, "ERR Private element '" + el.getSimpleName() + "' can't be annotated with @DesignerProperty.");
      }
    }

    String root = processingEnv.getOptions().get("root");
    String version = processingEnv.getOptions().get("version");
    String type = org + "." + extName;
    String output = processingEnv.getOptions().get("output");

    InfoFilesGenerator generator = new InfoFilesGenerator(root, version, type, extensionFieldInfo, output);
    try {
      generator.generateBuildInfoJson();
      generator.generateSimpleCompJson();
    } catch (IOException | ParserConfigurationException | SAXException | YamlReadingException e) {
      messager.printMessage(Diagnostic.Kind.ERROR, e.getMessage());
    }

    return false;
  }
}
