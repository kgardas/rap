/*******************************************************************************
 * Copyright (c) 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.rap.rwt.internal.resources;

import java.io.IOException;
import java.io.InputStream;

import org.eclipse.rap.rwt.RWT;
import org.eclipse.rap.rwt.internal.util.ClassUtil;
import org.eclipse.rap.rwt.resources.IResourceManager;
import org.eclipse.rap.rwt.service.IApplicationStore;


public class JavaScriptLoaderImpl implements JavaScriptLoader {

  private static final String MODULES_KEY = JavaScriptModuleRegistry.class.getName() + "#instance";

  public void ensureModule( Class< ? extends JavaScriptModule> type ) {
    if( !isRegistered( type ) ) {
      registerModule( type );
    }
  }

  private void registerModule( Class< ? extends JavaScriptModule> type ) {
    JavaScriptModule module = ClassUtil.newInstance( type );
    String[] fileNames = module.getFileNames();
    try {
      for( int i = 0; i < fileNames.length; i++ ) {
        registerFile( module, fileNames[ i ] );
      }
    } catch( IOException ioe ) {
      throw new IllegalArgumentException( "Failed to load resources", ioe );
    }
    getApplicationModules().put( module );
  }

  private static void registerFile( JavaScriptModule module, String fileName ) throws IOException {
    String localPath = getLocalPath( module, fileName );
    InputStream inputStream = module.getLoader().getResourceAsStream( localPath );
    IResourceManager resourceManager = RWT.getResourceManager();
    try {
      // TODO [tb] : ensure that content is not concatenated to core js library
      resourceManager.register( getPublicPath( module, fileName ), inputStream );
    } finally {
      if( inputStream != null ) {
        inputStream.close();
      }
    }
  }

  private static String getPublicPath( JavaScriptModule module, String fileName ) {
    Class type = module.getClass();
    String result =   type.getSimpleName()
                    + String.valueOf( type.hashCode() )
                    + "/"
                    + fileName;
    return result;
  }

  private static String getLocalPath( JavaScriptModule module, String fileName ) {
    return module.getDirectory() + fileName;
  }

  private static boolean isRegistered( Class<? extends JavaScriptModule> clazz ) {
    return getApplicationModules().get( clazz ) != null;
  }

  private static JavaScriptModuleRegistry getApplicationModules() {
    IApplicationStore store = RWT.getApplicationStore();
    JavaScriptModuleRegistry result = ( JavaScriptModuleRegistry )store.getAttribute( MODULES_KEY );
    if( result == null ) {
      result = new JavaScriptModuleRegistry();
      store.setAttribute( MODULES_KEY, result );
    }
    return result;
  }

}
