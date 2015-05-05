/**
 * Copyright (c) 2010-2015, openHAB.org and others.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 */
package org.openhab.binding.tinkerforge.internal.model;

import com.tinkerforge.BrickletSegmentDisplay4x7;

/**
 * <!-- begin-user-doc -->
 * A representation of the model object '<em><b>MBricklet Segment Display4x7</b></em>'.
 * 
 * @author Theo Weiss
 * @since 1.5.0
 * <!-- end-user-doc -->
 *
 * <p>
 * The following features are supported:
 * <ul>
 *   <li>{@link org.openhab.binding.tinkerforge.internal.model.MBrickletSegmentDisplay4x7#getDeviceType <em>Device Type</em>}</li>
 * </ul>
 * </p>
 *
 * @see org.openhab.binding.tinkerforge.internal.model.ModelPackage#getMBrickletSegmentDisplay4x7()
 * @model superTypes="org.openhab.binding.tinkerforge.internal.model.NumberActor org.openhab.binding.tinkerforge.internal.model.MDevice<org.openhab.binding.tinkerforge.internal.model.TinkerBrickletSegmentDisplay4x7>"
 * @generated
 */
public interface MBrickletSegmentDisplay4x7 extends NumberActor, MDevice<BrickletSegmentDisplay4x7>
{

  /**
   * Returns the value of the '<em><b>Device Type</b></em>' attribute.
   * The default value is <code>"bricklet_segmentdisplay4x7"</code>.
   * <!-- begin-user-doc -->
   * <p>
   * If the meaning of the '<em>Device Type</em>' attribute isn't clear,
   * there really should be more of a description here...
   * </p>
   * <!-- end-user-doc -->
   * @return the value of the '<em>Device Type</em>' attribute.
   * @see org.openhab.binding.tinkerforge.internal.model.ModelPackage#getMBrickletSegmentDisplay4x7_DeviceType()
   * @model default="bricklet_segmentdisplay4x7" unique="false" changeable="false"
   * @generated
   */
  String getDeviceType();
} // MBrickletSegmentDisplay4x7
