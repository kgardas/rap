/*******************************************************************************
 * Copyright (c) 2009, 2012 EclipseSource and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *    EclipseSource - initial API and implementation
 ******************************************************************************/
package org.eclipse.swt.internal.widgets.displaykit;

import java.util.ArrayList;

import org.eclipse.rap.rwt.internal.lifecycle.LifeCycleUtil;
import org.eclipse.rap.rwt.internal.protocol.ClientMessage;
import org.eclipse.rap.rwt.internal.protocol.ClientMessage.NotifyOperation;
import org.eclipse.rap.rwt.internal.protocol.ClientMessageConst;
import org.eclipse.rap.rwt.internal.protocol.ProtocolUtil;
import org.eclipse.rap.rwt.internal.util.NumberFormatUtil;
import org.eclipse.rap.rwt.lifecycle.ProcessActionRunner;
import org.eclipse.rap.rwt.lifecycle.WidgetUtil;
import org.eclipse.swt.dnd.*;
import org.eclipse.swt.events.DragDetectEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.internal.dnd.IDNDAdapter;
import org.eclipse.swt.internal.widgets.IDisplayAdapter;
import org.eclipse.swt.widgets.*;

// TODO [rh] move these methods to DragSourceLCA
public final class DNDSupport {

  private static final String EVENT_DRAG_START = "dragStart";
  private static final String EVENT_DRAG_ENTER = "dragEnter";
  private static final String EVENT_DRAG_OPERATION_CHANGED = "dragOperationChanged";
  private static final String EVENT_DRAG_OVER = "dragOver";
  private static final String EVENT_DRAG_LEAVE = "dragLeave";
  private static final String EVENT_DROP_ACCEPT = "dropAccept";
  private static final String EVENT_DRAG_FINISHED = "dragFinished";

  private static final String EVENT_PARAM_OPERATION = "operation";
  private static final String EVENT_PARAM_ITEM = "item";
  private static final String EVENT_PARAM_TIME = "time";
  private static final String EVENT_PARAM_SOURCE = "source";
  private static final String EVENT_PARAM_FEEDBACK = "feedback";
  private static final String EVENT_PARAM_DATATYPE = "dataType";

  private DNDSupport() {
    // prevent instantiation
  }

  public static void processEvents() {
    ProcessActionRunner.add( new Runnable() {
      public void run() {
        processDragStart();
        if( isLeaveBeforeEnter() ) {
          processDragLeave();
          processDragEnter();
          processDragOperationChanged();
          processDragOver();
        } else {
          processDragEnter();
          processDragOperationChanged();
          processDragOver();
          processDragLeave();
        }
        processDragFinished();
      }
    } );
  }

  private static void processDragStart() {
    ClientMessage message = ProtocolUtil.getClientMessage();
    NotifyOperation notify = message.getLastNotifyOperationFor( null, EVENT_DRAG_START );
    if( notify != null ) {
      Control control = ( Control )findWidgetById( notify.getTarget() );
      DragSource dragSource = getDragSource( control );
      Point point = readXYParams( notify ); // should be changed to array
      Point mappedPoint = control.getDisplay().map( null, control, point );
      DragDetectEvent dragDetectEvent = createDragDetectEvent( notify, control, mappedPoint );
      dragDetectEvent.processEvent();
      DragSourceEvent dragStartEvent
      = createDragStartEvent( dragSource, mappedPoint, dragDetectEvent.time );
      dragStartEvent.processEvent();
      if( dragStartEvent.doit == false ) {
        getDNDAdapter( dragSource ).cancel();
      }
    }
  }

  private static void processDragEnter() {
    ClientMessage message = ProtocolUtil.getClientMessage();
    NotifyOperation notify = message.getLastNotifyOperationFor( null, EVENT_DRAG_ENTER );
    if( notify != null ) {
      Control control = ( Control )findWidgetById( notify.getTarget() );
      DropTarget dropTarget = getDropTarget( control );
      Control sourceControl = readControlParam( notify );
      DragSource dragSource = getDragSource( sourceControl );
      Point point = readXYParams( notify );
      DropTargetEvent event = new DropTargetEvent( dropTarget, DropTargetEvent.DRAG_ENTER );
      int operation = readOperationParam( notify );
      int feedback = readIntParam( notify, EVENT_PARAM_FEEDBACK );
      Item item = readItemParam( notify );
      TransferData[] validDataTypes = determineDataTypes( dragSource, dropTarget );
      TransferData dataType = validDataTypes[ 0 ];
      event.detail = operation;
      event.operations = getOperations( dragSource, dropTarget );
      event.feedback = feedback;
      event.currentDataType = dataType;
      event.dataTypes = validDataTypes;
      event.item = item;
      event.x = point.x;
      event.y = point.y;
      event.time = readIntParam( notify, EVENT_PARAM_TIME );
      event.processEvent();
      if( event.detail != operation ) {
        changeOperation( dragSource, dropTarget, event.detail );
      }
      // no check, dataType is always changed from null to a valid value:
      changeDataType( dragSource, dropTarget, event.currentDataType );
      if( event.feedback != feedback ) {
        getDNDAdapter( dragSource ).setFeedbackChanged( control, event.feedback );
      }
    }
  }

  private static void processDragOver() {
    ClientMessage message = ProtocolUtil.getClientMessage();
    NotifyOperation notify = message.getLastNotifyOperationFor( null, EVENT_DRAG_OVER );
    if( notify != null ) {
      Control control = ( Control )findWidgetById( notify.getTarget() );
      DropTarget dropTarget = getDropTarget( control );
      Control sourceControl = readControlParam( notify );
      DragSource dragSource = getDragSource( sourceControl );
      IDNDAdapter dndAdapter = getDNDAdapter( dragSource );
      int operation = readOperationParam( dndAdapter, notify );
      int feedback = readFeedbackParam( dndAdapter, notify );
      TransferData dataType = readTransferDataParam( dndAdapter, notify );
      Point point = readXYParams( notify );
      Item item = readItemParam( notify );
      DropTargetEvent event = new DropTargetEvent( dropTarget, DropTargetEvent.DRAG_OVER );
      event.detail = operation;
      event.feedback = feedback;
      event.operations = getOperations( dragSource, dropTarget );
      event.currentDataType = dataType;
      event.dataTypes = determineDataTypes( dragSource, dropTarget );
      event.item = item;
      event.x = point.x;
      event.y = point.y;
      event.time = readIntParam( notify, EVENT_PARAM_TIME );
      event.processEvent();
      if( event.detail != operation ) {
        changeOperation( dragSource, dropTarget, event.detail );
      }
      if( event.currentDataType != dataType ) {
        changeDataType( dragSource, dropTarget, event.currentDataType );
      }
      if( event.feedback != feedback ) {
        getDNDAdapter( dragSource ).setFeedbackChanged( control, event.feedback );
      }
    }
  }


  private static void processDragOperationChanged() {
    ClientMessage message = ProtocolUtil.getClientMessage();
    NotifyOperation notify = message.getLastNotifyOperationFor( null, EVENT_DRAG_OPERATION_CHANGED );
    if( notify != null ) {
      Control control = ( Control )findWidgetById( notify.getTarget() );
      DropTarget dropTarget = getDropTarget( control );
      Control sourceControl = readControlParam( notify );
      DragSource dragSource = getDragSource( sourceControl );
      IDNDAdapter dndAdapter = getDNDAdapter( dragSource );
      int operation = readOperationParam( dndAdapter, notify );
      int feedback = readFeedbackParam( dndAdapter, notify );
      TransferData dataType = readTransferDataParam( dndAdapter, notify );
      Point point = readXYParams( notify );
      Item item = readItemParam( notify );
      DropTargetEvent event
        = new DropTargetEvent( dropTarget, DropTargetEvent.DRAG_OPERATION_CHANGED );
      event.detail = operation;
      event.feedback = feedback;
      event.currentDataType = dataType;
      event.dataTypes = determineDataTypes( dragSource, dropTarget );
      event.operations = getOperations( dragSource, dropTarget );
      event.item = item;
      event.x = point.x;
      event.y = point.y;
      event.time = readIntParam( notify, EVENT_PARAM_TIME );
      event.processEvent();
      if( event.detail != operation ) {
        changeOperation( dragSource, dropTarget, event.detail );
      }
      if( event.currentDataType != dataType ) {
        changeDataType( dragSource, dropTarget, event.currentDataType );
      }
      if( event.feedback != feedback ) {
        getDNDAdapter( dragSource ).setFeedbackChanged( control, event.feedback );
      }
    }
  }

  private static void processDragLeave() {
    ClientMessage message = ProtocolUtil.getClientMessage();
    NotifyOperation notify = message.getLastNotifyOperationFor( null, EVENT_DRAG_LEAVE );
    if( notify != null ) {
      Control control = ( Control )findWidgetById( notify.getTarget() );
      DropTarget dropTarget = getDropTarget( control );
      Point point = readXYParams( notify );
      int operation = readOperationParam( notify );
      int time = readIntParam( notify, EVENT_PARAM_TIME );
      fireDragLeave( operation, dropTarget, point, time );
    }
  }

  private static void processDragFinished() {
    int operation = DND.DROP_NONE;
    ClientMessage message = ProtocolUtil.getClientMessage();
    NotifyOperation notify = message.getLastNotifyOperationFor( null, EVENT_DROP_ACCEPT );
    if( notify != null ) {
      Control dropTargetControl = ( Control )findWidgetById( notify.getTarget() );
      DropTarget dropTarget = getDropTarget( dropTargetControl );
      Control sourceControl = readControlParam( notify );
      DragSource dragSource = getDragSource( sourceControl );
      IDNDAdapter dndAdapter = getDNDAdapter( dragSource );
      operation = readOperationParam( dndAdapter, notify );
      TransferData dataType = readTransferDataParam( dndAdapter, notify );
      Point point = readXYParams( notify );
      Item item = readItemParam( notify );
      int time = readIntParam( notify, EVENT_PARAM_TIME );
      // fire DRAG_LEAVE, which is suppressed by the client
      fireDragLeave( operation, dropTarget, point, time );
      // fire DROP_ACCEPT
      DropTargetEvent event
        = createDropAcceptEvent( dropTarget, operation, point, dataType, item );
      event.operations = getOperations( dragSource, dropTarget );
      event.processEvent();
      operation = checkOperation( dragSource, dropTarget, event.detail );
      TransferData[] validDataTypes = determineDataTypes( dragSource, dropTarget );
      dataType = checkDataType( event.currentDataType, validDataTypes );
      if( operation != DND.DROP_NONE && dataType != null ) {
        // fire DRAG_SET_DATA
        DragSourceEvent setDataEvent = createDragSetDataEvent( dragSource, dataType, point );
        setDataEvent.processEvent();
        // Check data
        Object data = transferData( dropTarget, dataType, setDataEvent );
        // fire DROP
        DropTargetEvent dropEvent = new DropTargetEvent( dropTarget, DropTargetEvent.DROP );
        dropEvent.detail = operation;
        dropEvent.operations = getOperations( dragSource, dropTarget );
        dropEvent.currentDataType = dataType;
        dropEvent.dataTypes = validDataTypes;
        dropEvent.item = item;
        dropEvent.x = point.x;
        dropEvent.y = point.y;
        dropEvent.data = data;
        dropEvent.processEvent();
        operation = checkOperation( dragSource, dropTarget, dropEvent.detail );
      }
    }
    fireDragFinished( operation );
  }

  //////////////////////////
  // Create and fire events

  private static DragDetectEvent createDragDetectEvent( NotifyOperation operation,
                                                        Control control,
                                                        Point point )
  {
    DragDetectEvent result = new DragDetectEvent( control );
    result.x = point.x;
    result.y = point.y;
    result.button = 1;
    result.time = readIntParam( operation, EVENT_PARAM_TIME );
    return result;
  }

  private static DragSourceEvent createDragStartEvent(
    DragSource dragSource,
    Point point,
    int time )
  {
    DragSourceEvent result = new DragSourceEvent( dragSource, DragSourceEvent.DRAG_START );
    result.detail = DND.DROP_NONE;
    result.x = point.x;
    result.y = point.y;
    result.doit = true;
    result.time = time;
    return result;
  }

  private static DragSourceEvent createDragSetDataEvent(
    DragSource dragSource,
    TransferData dataType,
    Point point )
  {
    DragSourceEvent result = new DragSourceEvent( dragSource, DragSourceEvent.DRAG_SET_DATA );
    result.detail = DND.DROP_NONE;
    result.dataType = dataType;
    result.x = point.x;
    result.y = point.y;
    result.data = null;
    result.doit = true;
    return result;
  }

  private static DropTargetEvent createDropAcceptEvent(
    DropTarget dropTarget,
    int operation,
    Point point,
    TransferData dataType,
    Item item )
  {
    DropTargetEvent result = new DropTargetEvent( dropTarget, DropTargetEvent.DROP_ACCEPT );
    result.detail = operation;
    result.x = point.x;
    result.y = point.y;
    result.item = item;
    result.currentDataType = dataType;
    return result;
  }

  private static void fireDragLeave( int operation,
                                     DropTarget dropTarget,
                                     Point point,
                                     int time )
  {
    DropTargetEvent event = new DropTargetEvent( dropTarget, DropTargetEvent.DRAG_LEAVE );
    event.detail = operation;
    event.x = point.x;
    event.y = point.y;
    event.time = time;
    event.processEvent();
  }

  private static void fireDragFinished( int operation ) {
    ClientMessage message = ProtocolUtil.getClientMessage();
    NotifyOperation notify = message.getLastNotifyOperationFor( null, EVENT_DRAG_FINISHED );
    if( notify != null ) {
      Control dragSourceControl = ( Control )findWidgetById( notify.getTarget() );
      // fire DRAG_END
      DragSource dragSource = getDragSource( dragSourceControl );
      IDNDAdapter dndAdapter = getDNDAdapter( dragSource );
      dndAdapter.cancelDetailChanged();
      dndAdapter.cancelFeedbackChanged();
      dndAdapter.cancelDataTypeChanged();
      Point point = readXYParams( notify );
      DragSourceEvent event = new DragSourceEvent( dragSource, DragSourceEvent.DRAG_END );
      event.x = point.x;
      event.y = point.y;
      event.detail = operation;
      // NOTE : Doit is always true in SWT/Win, but should be false
      //        if no drop occurred. (According to documentation.)
      event.doit = true;
      event.time = readIntParam( notify, EVENT_PARAM_TIME );
      event.processEvent();
    }
  }

  //////////////////
  // Helping methods

  private static IDNDAdapter getDNDAdapter( DragSource dragSource ) {
    return dragSource.getAdapter( IDNDAdapter.class );
  }

  private static DropTarget getDropTarget( Control control ) {
    return ( DropTarget )control.getData( DND.DROP_TARGET_KEY );
  }

  private static DragSource getDragSource( Control control ) {
    return ( DragSource )control.getData( DND.DRAG_SOURCE_KEY );
  }

  private static Object transferData( DropTarget dropTarget,
                                      TransferData dataType,
                                      DragSourceEvent setDataEvent )
  {
    Object data = null;
    if( setDataEvent.doit ) {
      Transfer transfer = findTransferByType( dataType, dropTarget );
      transfer.javaToNative( setDataEvent.data, dataType );
      data = transfer.nativeToJava( dataType );
    }
    return data;
  }

  static TransferData[] determineDataTypes( DragSource dragSource, DropTarget dropTarget ) {
    java.util.List<TransferData> supportedTypes = new ArrayList<TransferData>();
    Transfer[] dragSourceTransfers = dragSource.getTransfer();
    Transfer[] dropTargetTransfers = dropTarget.getTransfer();
    for( int i = 0; i < dragSourceTransfers.length; i++ ) {
      TransferData[] dataTypes = dragSourceTransfers[ i ].getSupportedTypes();
      for( int j = 0; j < dropTargetTransfers.length; j++ ) {
        for( int k = 0; k < dataTypes.length; k++ ) {
          if( dropTargetTransfers[ j ].isSupportedType( dataTypes[ k ] ) ) {
            supportedTypes.add( dataTypes[ k ] );
          }
        }
      }
    }
    TransferData[] result = new TransferData[ supportedTypes.size() ];
    for( int i = 0; i < supportedTypes.size(); i++ ) {
      result[ i ] = supportedTypes.get( i );
    }
    return result;
  }

  private static Transfer findTransferByType( TransferData type, DropTarget dropTarget ) {
    Transfer result = null;
    Transfer[] supported = dropTarget.getTransfer();
    for( int i = 0; result == null && i < supported.length; i++ ) {
      if( supported[ i ].isSupportedType( type ) ) {
        result = supported[ i ];
      }
    }
    return result;
  }

  private static Widget findWidgetById( String id ) {
    Widget result = null;
    Display display = LifeCycleUtil.getSessionDisplay();
    Shell[] shells = getDisplayAdapter( display ).getShells();
    for( int i = 0; result == null && i < shells.length; i++ ) {
      Widget widget = WidgetUtil.find( shells[ i ], id );
      if( widget != null ) {
        result = widget;
      }
    }
    return result;
  }

  private static IDisplayAdapter getDisplayAdapter( Display display ) {
    Object adapter = display.getAdapter( IDisplayAdapter.class );
    return ( IDisplayAdapter )adapter;
  }

  private static String readStringParam( NotifyOperation operation, String property ) {
    String result = null;
    Object value = operation.getProperty( property );
    if( value != null ) {
      result = value.toString();
    }
    return result;
  }

  private static int readIntParam( NotifyOperation operation, String property ) {
    String value = readStringParam( operation, property );
    return NumberFormatUtil.parseInt( value );
  }

  private static Control readControlParam( NotifyOperation operation ) {
    Control result = null;
    String value = ( String )operation.getProperty( EVENT_PARAM_SOURCE );
    if( value != null ) {
      result = ( Control )findWidgetById( value );
    }
    return result;
  }

  private static Item readItemParam( NotifyOperation operation ) {
    Item result = null;
    String value = readStringParam( operation, EVENT_PARAM_ITEM );
    if( value != null ) {
      result = ( Item )findWidgetById( value );
    }
    return result;
  }

  private static TransferData readDataTypeParam( NotifyOperation operation ) {
    TransferData result = null;
    String value = readStringParam( operation, EVENT_PARAM_DATATYPE );
    value = "null".equals( value ) ? null : value;
    if( value != null ) {
      result = new TransferData();
      result.type = NumberFormatUtil.parseInt( value );
    }
    return result;
  }

  // DND TODO [tb] : Is a check needed (like checkAndProcessMouseEvent)?
  //                 Yes, a drag would have to be canceled for invalid
  //                 coordinates on DragDetect, and dragEnter/Leave COULD
  //                 be thrown before they would be in SWT. (Severe problem?)
  private static Point readXYParams( NotifyOperation operation ) {
    int x = readIntParam( operation, ClientMessageConst.EVENT_PARAM_X );
    int y = readIntParam( operation, ClientMessageConst.EVENT_PARAM_Y );
    return new Point( x, y );
  }

  private static int readOperationParam( IDNDAdapter dndAdapter, NotifyOperation operation ) {
    int result;
    if( dndAdapter.hasDetailChanged() ) {
      result = dndAdapter.getDetailChangedValue();
    } else {
      result = readOperationParam( operation );
    }
    return result;
  }

  private static int readFeedbackParam( IDNDAdapter dndAdapter, NotifyOperation operation ) {
    int result;
    if( dndAdapter.hasFeedbackChanged() ) {
      result = dndAdapter.getFeedbackChangedValue();
    } else {
      result = readIntParam( operation, EVENT_PARAM_FEEDBACK );
    }
    return result;
  }

  private static TransferData readTransferDataParam( IDNDAdapter adapter,
                                                     NotifyOperation operation )
  {
    TransferData result;
    if( adapter.hasDataTypeChanged() ) {
      result = adapter.getDataTypeChangedValue();
    } else {
      result = readDataTypeParam( operation );
    }
    return result;
  }

  private static int readOperationParam( NotifyOperation operation ) {
    int result = DND.DROP_NONE;
    String value = readStringParam( operation, EVENT_PARAM_OPERATION );
    if( "copy".equals( value ) ) {
      result = DND.DROP_COPY;
    } else if( "move".equals( value ) ) {
      result = DND.DROP_MOVE;
    } else if( "link".equals( value ) ) {
      result = DND.DROP_LINK;
    }
    return result;
  }

  private static boolean isLeaveBeforeEnter() {
    boolean result = false;
    ClientMessage message = ProtocolUtil.getClientMessage();
    NotifyOperation enter = message.getLastNotifyOperationFor( null, EVENT_DRAG_ENTER );
    NotifyOperation leave = message.getLastNotifyOperationFor( null, EVENT_DRAG_LEAVE );
    if( enter != null && leave != null ) {
      int enterTime = readIntParam( enter, EVENT_PARAM_TIME );
      int leaveTime = readIntParam( leave, EVENT_PARAM_TIME );
      result = leaveTime <= enterTime;
    }
    return result;
  }

  private static void changeOperation( DragSource dragSource, DropTarget dropTarget, int detail ) {
    int checkedOperation = checkOperation( dragSource, dropTarget, detail );
    IDNDAdapter dndAdapter = getDNDAdapter( dragSource );
    dndAdapter.setDetailChanged( dropTarget.getControl(), checkedOperation );
  }

  private static void changeDataType( DragSource dragSource,
                                      DropTarget dropTarget,
                                      TransferData dataType )
  {

    TransferData[] validDataTypes = determineDataTypes( dragSource, dropTarget );
    TransferData value = checkDataType( dataType, validDataTypes );
    // [tb] If the value is not valid, another valid value will be set.
    // This is simplified from SWT, where null would be set.
    if( value == null ) {
      value = validDataTypes[ 0 ];
    }
    Control control = dropTarget.getControl();
    getDNDAdapter( dragSource ).setDataTypeChanged( control, value );
  }

  private static int getOperations( DragSource dragSource, DropTarget dropTarget ) {
    return dragSource.getStyle() & dropTarget.getStyle();
  }

  private static int checkOperation( DragSource dragSource, DropTarget dropTarget, int operation ) {
    int result = DND.DROP_NONE;
    int allowedOperations = getOperations( dragSource, dropTarget );
    if( ( allowedOperations & operation ) != 0 ) {
      result = operation;
    }
    return result;
  }

  private static TransferData checkDataType( TransferData dataType, TransferData[] validTypes ) {
    boolean isValidType = false;
    for( int i = 0; i < validTypes.length; i++ ) {
      if( !isValidType ) {
        isValidType = TransferData.sameType( dataType, validTypes[ i ] );
      }
    }
    return isValidType ? dataType : null;
  }
}

