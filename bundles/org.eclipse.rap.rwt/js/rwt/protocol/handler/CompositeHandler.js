/*******************************************************************************
 * Copyright (c) 2011, 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/

rwt.protocol.HandlerRegistry.add( "rwt.widgets.Composite", {

  factory : function( properties ) {
    var result = new rwt.widgets.Composite();
    rwt.protocol.HandlerUtil.addStatesForStyles( result, properties.style );
    result.setUserData( "isControl", true );
    rwt.protocol.HandlerUtil.setParent( result, properties.parent );
    return result;
  },

  destructor : rwt.protocol.HandlerUtil.getControlDestructor(),

  getDestroyableChildren : rwt.protocol.HandlerUtil.getDestroyableChildrenFinder(),

  properties : rwt.protocol.HandlerUtil.extendControlProperties( [
    "backgroundGradient",
    "roundedBorder"
  ] ),

  propertyHandler : rwt.protocol.HandlerUtil.extendControlPropertyHandler( {
    "backgroundGradient" : rwt.protocol.HandlerUtil.getBackgroundGradientHandler(),
    "roundedBorder" : rwt.protocol.HandlerUtil.getRoundedBorderHandler()
  } ),

  listeners : rwt.protocol.HandlerUtil.extendControlListeners( [] ),

  listenerHandler : rwt.protocol.HandlerUtil.extendControlListenerHandler( {} )

} );