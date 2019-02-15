package com.devonfw.ide.configurator.merge;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.util.Properties;
import java.util.Set;

import com.devonfw.ide.configurator.SortedProperties;
import com.devonfw.ide.configurator.logging.Log;
import com.devonfw.ide.configurator.resolve.VariableResolver;

/**
 * Implementation of {@link FileTypeMerger} for {@link Properties} files.
 *
 * @since 3.0.0
 */
public class PropertiesMerger extends FileTypeMerger {

  private static final String ENCODING = "UTF-8";

  @Override
  public void merge(File setupFile, File updateFile, VariableResolver resolver, File workspaceFile) {

    SortedProperties properties = new SortedProperties();
    boolean updateFileExists = updateFile.exists();
    if (workspaceFile.exists()) {
      if (!updateFileExists) {
        Log.LOGGER.finer("Nothing to do as update file does not exist: " + updateFile);
        return; // nothing to do ...
      }
      load(properties, workspaceFile);
    } else if (setupFile.exists()) {
      load(properties, setupFile);
    }
    if (updateFileExists) {
      load(properties, updateFile);
    }
    resolve(properties, resolver);
    save(properties, workspaceFile);
    Log.LOGGER.finer("Saved merged properties to: " + workspaceFile);
  }

  public static Properties load(File file) {

    Properties properties = new Properties();
    load(properties, file);
    return properties;
  }

  public static Properties loadIfExists(File file) {

    Properties properties = new Properties();
    if ((file != null) && file.exists()) {
      load(properties, file);
    } else {
      Log.LOGGER.finest("Properties file does not exist: " + file);
    }
    return properties;
  }

  public static void load(Properties properties, File file) {

    Log.LOGGER.finest("Loading properties file " + file);
    try (InputStream in = new FileInputStream(file); Reader reader = new InputStreamReader(in, ENCODING)) {
      properties.load(reader);
    } catch (IOException e) {
      throw new IllegalStateException("Could not load properties from file: " + file, e);
    }
  }

  private void resolve(Properties properties, VariableResolver resolver) {

    Set<Object> keys = properties.keySet();
    for (Object key : keys) {
      String value = properties.getProperty(key.toString());
      properties.setProperty(key.toString(), resolver.resolve(value));
    }
  }

  public static void save(Properties properties, File file) {

    Log.LOGGER.finest("Saving properties file " + file);
    ensureParentDirecotryExists(file);
    try (OutputStream out = new FileOutputStream(file); Writer writer = new OutputStreamWriter(out, ENCODING)) {
      properties.store(writer, null);
    } catch (IOException e) {
      throw new IllegalStateException("Could not write properties to file: " + file, e);
    }
  }

  @Override
  public void inverseMerge(File workspaceFile, VariableResolver resolver, boolean addNewProperties, File updateFile) {

    if (!workspaceFile.exists()) {
      Log.LOGGER.finest("Workspace file does not exist: " + workspaceFile.getAbsolutePath());
      return;
    }
    if (!updateFile.exists()) {
      Log.LOGGER.finest("Update file does not exist: " + updateFile.getAbsolutePath());
      return;
    }
    Properties updateProperties = load(updateFile);
    Properties workspaceProperties = load(workspaceFile);
    SortedProperties mergedProperties = new SortedProperties();
    mergedProperties.putAll(updateProperties);
    boolean updated = false;
    for (Object key : workspaceProperties.keySet()) {
      Object workspaceValue = workspaceProperties.get(key);
      Object updateValue = updateProperties.get(key);
      if ((updateValue != null) || addNewProperties) {
        String updateValueResolved = null;
        if (updateValue != null) {
          updateValueResolved = resolver.resolve(updateValue.toString());
        }
        if (!workspaceValue.equals(updateValueResolved)) {
          String workspaceValueInverseResolved = resolver.inverseResolve(workspaceValue.toString());
          mergedProperties.put(key, workspaceValueInverseResolved);
          updated = true;
        }
      }
    }
    if (updated) {
      save(mergedProperties, updateFile);
      Log.LOGGER.info("Saved changes in " + workspaceFile.getName() + " to " + updateFile.getAbsolutePath());
    } else {
      Log.LOGGER.finest("No changes for " + updateFile.getAbsolutePath());
    }
  }

}
