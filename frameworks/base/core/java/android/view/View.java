/*
 * Copyright (C) 2006 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package android.view;

import static android.content.res.Resources.ID_NULL;
import static android.os.Trace.TRACE_TAG_APP;
import static android.os.Trace.TRACE_TAG_VIEW;
import static android.service.autofill.Flags.FLAG_AUTOFILL_CREDMAN_DEV_INTEGRATION;
import static android.view.ContentInfo.SOURCE_DRAG_AND_DROP;
import static android.view.Surface.FRAME_RATE_CATEGORY_HIGH;
import static android.view.Surface.FRAME_RATE_CATEGORY_HIGH_HINT;
import static android.view.Surface.FRAME_RATE_CATEGORY_LOW;
import static android.view.Surface.FRAME_RATE_CATEGORY_NORMAL;
import static android.view.Surface.FRAME_RATE_CATEGORY_NO_PREFERENCE;
import static android.view.Surface.FRAME_RATE_COMPATIBILITY_FIXED_SOURCE;
import static android.view.Surface.FRAME_RATE_COMPATIBILITY_GTE;
import static android.view.WindowManager.LayoutParams.TYPE_INPUT_METHOD;
import static android.view.accessibility.AccessibilityEvent.CONTENT_CHANGE_TYPE_UNDEFINED;
import static android.view.accessibility.Flags.FLAG_DEPRECATE_ACCESSIBILITY_ANNOUNCEMENT_APIS;
import static android.view.accessibility.Flags.FLAG_SUPPLEMENTAL_DESCRIPTION;
import static android.view.accessibility.Flags.removeChildHoverCheckForTouchExploration;
import static android.view.accessibility.Flags.supplementalDescription;
import static android.view.accessibility.Flags.supportMultipleLabeledby;
import static android.view.displayhash.DisplayHashResultCallback.DISPLAY_HASH_ERROR_INVALID_BOUNDS;
import static android.view.displayhash.DisplayHashResultCallback.DISPLAY_HASH_ERROR_MISSING_WINDOW;
import static android.view.displayhash.DisplayHashResultCallback.DISPLAY_HASH_ERROR_NOT_VISIBLE_ON_SCREEN;
import static android.view.displayhash.DisplayHashResultCallback.DISPLAY_HASH_ERROR_UNKNOWN;
import static android.view.displayhash.DisplayHashResultCallback.EXTRA_DISPLAY_HASH;
import static android.view.displayhash.DisplayHashResultCallback.EXTRA_DISPLAY_HASH_ERROR_CODE;
import static android.view.flags.Flags.FLAG_SENSITIVE_CONTENT_APP_PROTECTION_API;
import static android.view.flags.Flags.FLAG_TOOLKIT_SET_FRAME_RATE_READ_ONLY;
import static android.view.flags.Flags.FLAG_VIEW_VELOCITY_API;
import static android.view.flags.Flags.calculateBoundsInParentFromBoundsInScreen;
import static android.view.flags.Flags.enableUseMeasureCacheDuringForceLayout;
import static android.view.flags.Flags.sensitiveContentAppProtection;
import static android.view.flags.Flags.toolkitFrameRateAnimationBugfix25q1;
import static android.view.flags.Flags.toolkitFrameRateBySizeReadOnly;
import static android.view.flags.Flags.toolkitFrameRateDefaultNormalReadOnly;
import static android.view.flags.Flags.toolkitFrameRateSmallUsesPercentReadOnly;
import static android.view.flags.Flags.toolkitFrameRateVelocityMappingReadOnly;
import static android.view.flags.Flags.toolkitFrameRateViewEnablingReadOnly;
import static android.view.flags.Flags.toolkitMetricsForFrameRateDecision;
import static android.view.flags.Flags.toolkitSetFrameRateReadOnly;
import static android.view.flags.Flags.toolkitViewgroupSetRequestedFrameRateApi;
import static android.view.flags.Flags.viewVelocityApi;
import static android.view.inputmethod.Flags.FLAG_HOME_SCREEN_HANDWRITING_DELEGATOR;
import static android.view.inputmethod.Flags.initiationWithoutInputConnection;

import static com.android.internal.util.FrameworkStatsLog.TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__DEEP_PRESS;
import static com.android.internal.util.FrameworkStatsLog.TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__LONG_PRESS;
import static com.android.internal.util.FrameworkStatsLog.TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__SINGLE_TAP;
import static com.android.internal.util.FrameworkStatsLog.TOUCH_GESTURE_CLASSIFIED__CLASSIFICATION__UNKNOWN_CLASSIFICATION;
import static com.android.window.flags.Flags.FLAG_DELEGATE_UNHANDLED_DRAGS;
import static com.android.window.flags.Flags.FLAG_SUPPORTS_DRAG_ASSISTANT_TO_MULTIWINDOW;

import static java.lang.Math.max;

import android.animation.AnimatorInflater;
import android.animation.StateListAnimator;
import android.annotation.AttrRes;
import android.annotation.CallSuper;
import android.annotation.ColorInt;
import android.annotation.DrawableRes;
import android.annotation.FlaggedApi;
import android.annotation.FloatRange;
import android.annotation.IdRes;
import android.annotation.IntDef;
import android.annotation.IntRange;
import android.annotation.LayoutRes;
import android.annotation.NonNull;
import android.annotation.Nullable;
import android.annotation.RequiresPermission;
import android.annotation.Size;
import android.annotation.StyleRes;
import android.annotation.SuppressLint;
import android.annotation.SystemApi;
import android.annotation.TestApi;
import android.annotation.UiContext;
import android.annotation.UiThread;
import android.app.PendingIntent;
import android.app.jank.AppJankStats;
import android.app.jank.JankTracker;
import android.compat.annotation.UnsupportedAppUsage;
import android.content.AutofillOptions;
import android.content.ClipData;
import android.content.ClipDescription;
import android.content.Context;
import android.content.ContextWrapper;
import android.content.Intent;
import android.content.IntentSender;
import android.content.res.ColorStateList;
import android.content.res.CompatibilityInfo;
import android.content.res.Configuration;
import android.content.res.Resources;
import android.content.res.TypedArray;
import android.credentials.CredentialManager;
import android.credentials.CredentialOption;
import android.credentials.GetCredentialException;
import android.credentials.GetCredentialRequest;
import android.credentials.GetCredentialResponse;
import android.graphics.Bitmap;
import android.graphics.BlendMode;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.Interpolator;
import android.graphics.LinearGradient;
import android.graphics.Matrix;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.Point;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RecordingCanvas;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Region;
import android.graphics.RenderEffect;
import android.graphics.RenderNode;
import android.graphics.Shader;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.hardware.display.DisplayManagerGlobal;
import android.hardware.input.InputManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.OutcomeReceiver;
import android.os.Parcel;
import android.os.Parcelable;
import android.os.RemoteCallback;
import android.os.RemoteException;
import android.os.SystemClock;
import android.os.Trace;
import android.service.credentials.CredentialProviderService;
import android.sysprop.DisplayProperties;
import android.text.InputType;
import android.text.TextUtils;
import android.util.ArraySet;
import android.util.AttributeSet;
import android.util.DisplayMetrics;
import android.util.FloatProperty;
import android.util.LayoutDirection;
import android.util.Log;
import android.util.LongSparseArray;
import android.util.LongSparseLongArray;
import android.util.Pair;
import android.util.Pools.SynchronizedPool;
import android.util.Property;
import android.util.SparseArray;
import android.util.SparseIntArray;
import android.util.StateSet;
import android.util.SuperNotCalledException;
import android.util.TimeUtils;
import android.util.TypedValue;
import android.view.AccessibilityIterators.CharacterTextSegmentIterator;
import android.view.AccessibilityIterators.ParagraphTextSegmentIterator;
import android.view.AccessibilityIterators.TextSegmentIterator;
import android.view.AccessibilityIterators.WordTextSegmentIterator;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.InputDevice.InputSourceClass;
import android.view.Window.OnContentApplyWindowInsetsListener;
import android.view.WindowInsets.Type;
import android.view.WindowInsetsAnimation.Bounds;
import android.view.WindowManager.LayoutParams;
import android.view.accessibility.AccessibilityEvent;
import android.view.accessibility.AccessibilityEventSource;
import android.view.accessibility.AccessibilityManager;
import android.view.accessibility.AccessibilityNodeIdManager;
import android.view.accessibility.AccessibilityNodeInfo;
import android.view.accessibility.AccessibilityNodeInfo.AccessibilityAction;
import android.view.accessibility.AccessibilityNodeProvider;
import android.view.accessibility.AccessibilityWindowInfo;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.view.animation.Transformation;
import android.view.autofill.AutofillId;
import android.view.autofill.AutofillManager;
import android.view.autofill.AutofillValue;
import android.view.contentcapture.ContentCaptureContext;
import android.view.contentcapture.ContentCaptureManager;
import android.view.contentcapture.ContentCaptureSession;
import android.view.displayhash.DisplayHash;
import android.view.displayhash.DisplayHashManager;
import android.view.displayhash.DisplayHashResultCallback;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputConnection;
import android.view.inputmethod.InputMethodManager;
import android.view.inspector.InspectableProperty;
import android.view.inspector.InspectableProperty.EnumEntry;
import android.view.inspector.InspectableProperty.FlagEntry;
import android.view.translation.TranslationCapability;
import android.view.translation.TranslationSpec.DataFormat;
import android.view.translation.ViewTranslationCallback;
import android.view.translation.ViewTranslationRequest;
import android.view.translation.ViewTranslationResponse;
import android.widget.Checkable;
import android.widget.ScrollBarDrawable;
import android.window.OnBackInvokedDispatcher;

import com.android.internal.R;
import com.android.internal.util.ArrayUtils;
import com.android.internal.util.FrameworkStatsLog;
import com.android.internal.util.Preconditions;
import com.android.internal.view.ScrollCaptureInternal;
import com.android.internal.view.TooltipPopup;
import com.android.internal.view.menu.MenuBuilder;
import com.android.internal.widget.ScrollBarUtils;

import com.google.android.collect.Lists;
import com.google.android.collect.Maps;

import java.io.PrintWriter;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Predicate;
/**
 * <p>
 * This class represents the basic building block for user interface components. A View
 * occupies a rectangular area on the screen and is responsible for drawing and
 * event handling. View is the base class for <em>widgets</em>, which are
 * used to create interactive UI components (buttons, text fields, etc.). The
 * {@link android.view.ViewGroup} subclass is the base class for <em>layouts</em>, which
 * are invisible containers that hold other Views (or other ViewGroups) and define
 * their layout properties.
 * </p>
 *
 * <div class="special reference">
 * <h3>Developer Guides</h3>
 * <p>For information about using this class to develop your application's user interface,
 * read the <a href="{@docRoot}guide/topics/ui/index.html">User Interface</a> developer guide.
 * </div>
 *
 * <a name="Using"></a>
 * <h3>Using Views</h3>
 * <p>
 * All of the views in a window are arranged in a single tree. You can add views
 * either from code or by specifying a tree of views in one or more XML layout
 * files. There are many specialized subclasses of views that act as controls or
 * are capable of displaying text, images, or other content.
 * </p>
 * <p>
 * Once you have created a tree of views, there are typically a few types of
 * common operations you may wish to perform:
 * <ul>
 * <li><strong>Set properties:</strong> for example setting the text of a
 * {@link android.widget.TextView}. The available properties and the methods
 * that set them will vary among the different subclasses of views. Note that
 * properties that are known at build time can be set in the XML layout
 * files.</li>
 * <li><strong>Set focus:</strong> The framework will handle moving focus in
 * response to user input. To force focus to a specific view, call
 * {@link #requestFocus}.</li>
 * <li><strong>Set up listeners:</strong> Views allow clients to set listeners
 * that will be notified when something interesting happens to the view. For
 * example, all views will let you set a listener to be notified when the view
 * gains or loses focus. You can register such a listener using
 * {@link #setOnFocusChangeListener(android.view.View.OnFocusChangeListener)}.
 * Other view subclasses offer more specialized listeners. For example, a Button
 * exposes a listener to notify clients when the button is clicked.</li>
 * <li><strong>Set visibility:</strong> You can hide or show views using
 * {@link #setVisibility(int)}.</li>
 * </ul>
 * </p>
 * <p><em>
 * Note: The Android framework is responsible for measuring, laying out and
 * drawing views. You should not call methods that perform these actions on
 * views yourself unless you are actually implementing a
 * {@link android.view.ViewGroup}.
 * </em></p>
 *
 * <a name="Lifecycle"></a>
 * <h3>Implementing a Custom View</h3>
 *
 * <p>
 * To implement a custom view, you will usually begin by providing overrides for
 * some of the standard methods that the framework calls on all views. You do
 * not need to override all of these methods. In fact, you can start by just
 * overriding {@link #onDraw(android.graphics.Canvas)}.
 * <table border="2" width="85%" align="center" cellpadding="5">
 *     <thead>
 *         <tr><th>Category</th> <th>Methods</th> <th>Description</th></tr>
 *     </thead>
 *
 *     <tbody>
 *     <tr>
 *         <td rowspan="2">Creation</td>
 *         <td>Constructors</td>
 *         <td>There is a form of the constructor that are called when the view
 *         is created from code and a form that is called when the view is
 *         inflated from a layout file. The second form should parse and apply
 *         any attributes defined in the layout file.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td><code>{@link #onFinishInflate()}</code></td>
 *         <td>Called after a view and all of its children has been inflated
 *         from XML.</td>
 *     </tr>
 *
 *     <tr>
 *         <td rowspan="3">Layout</td>
 *         <td><code>{@link #onMeasure(int, int)}</code></td>
 *         <td>Called to determine the size requirements for this view and all
 *         of its children.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td><code>{@link #onLayout(boolean, int, int, int, int)}</code></td>
 *         <td>Called when this view should assign a size and position to all
 *         of its children.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td><code>{@link #onSizeChanged(int, int, int, int)}</code></td>
 *         <td>Called when the size of this view has changed.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *         <td>Drawing</td>
 *         <td><code>{@link #onDraw(android.graphics.Canvas)}</code></td>
 *         <td>Called when the view should render its content.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *         <td rowspan="6">Event processing</td>
 *         <td><code>{@link #onKeyDown(int, KeyEvent)}</code></td>
 *         <td>Called when a new hardware key event occurs.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td><code>{@link #onKeyUp(int, KeyEvent)}</code></td>
 *         <td>Called when a hardware key up event occurs.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td><code>{@link #onTrackballEvent(MotionEvent)}</code></td>
 *         <td>Called when a trackball motion event occurs.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td><code>{@link #onTouchEvent(MotionEvent)}</code></td>
 *         <td>Called when a motion event occurs with pointers down on the view.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td><code>{@link #onGenericMotionEvent(MotionEvent)}</code></td>
 *         <td>Called when a generic motion event occurs.
 *         </td>
 *     </tr>
 *     <tr>
 *         <td><code>{@link #onHoverEvent(MotionEvent)}</code></td>
 *         <td>Called when a hover motion event occurs.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *         <td rowspan="2">Focus</td>
 *         <td><code>{@link #onFocusChanged(boolean, int, android.graphics.Rect)}</code></td>
 *         <td>Called when the view gains or loses focus.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *         <td><code>{@link #onWindowFocusChanged(boolean)}</code></td>
 *         <td>Called when the window containing the view gains or loses focus.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *         <td rowspan="3">Attaching</td>
 *         <td><code>{@link #onAttachedToWindow()}</code></td>
 *         <td>Called when the view is attached to a window.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *         <td><code>{@link #onDetachedFromWindow}</code></td>
 *         <td>Called when the view is detached from its window.
 *         </td>
 *     </tr>
 *
 *     <tr>
 *         <td><code>{@link #onWindowVisibilityChanged(int)}</code></td>
 *         <td>Called when the visibility of the window containing the view
 *         has changed.
 *         </td>
 *     </tr>
 *     </tbody>
 *
 * </table>
 * </p>
 *
 * <a name="IDs"></a>
 * <h3>IDs</h3>
 * Views may have an integer id associated with them. These ids are typically
 * assigned in the layout XML files, and are used to find specific views within
 * the view tree. A common pattern is to:
 * <ul>
 * <li>Define a Button in the layout file and assign it a unique ID.
 * <pre>
 * &lt;Button
 *     android:id="@+id/my_button"
 *     android:layout_width="wrap_content"
 *     android:layout_height="wrap_content"
 *     android:text="@string/my_button_text"/&gt;
 * </pre></li>
 * <li>From the onCreate method of an Activity, find the Button
 * <pre class="prettyprint">
 *      Button myButton = findViewById(R.id.my_button);
 * </pre></li>
 * </ul>
 * <p>
 * View IDs need not be unique throughout the tree, but it is good practice to
 * ensure that they are at least unique within the part of the tree you are
 * searching.
 * </p>
 *
 * <a name="Position"></a>
 * <h3>Position</h3>
 * <p>
 * The geometry of a view is that of a rectangle. A view has a location,
 * expressed as a pair of <em>left</em> and <em>top</em> coordinates, and
 * two dimensions, expressed as a width and a height. The unit for location
 * and dimensions is the pixel.
 * </p>
 *
 * <p>
 * It is possible to retrieve the location of a view by invoking the methods
 * {@link #getLeft()} and {@link #getTop()}. The former returns the left, or X,
 * coordinate of the rectangle representing the view. The latter returns the
 * top, or Y, coordinate of the rectangle representing the view. These methods
 * both return the location of the view relative to its parent. For instance,
 * when getLeft() returns 20, that means the view is located 20 pixels to the
 * right of the left edge of its direct parent.
 * </p>
 *
 * <p>
 * In addition, several convenience methods are offered to avoid unnecessary
 * computations, namely {@link #getRight()} and {@link #getBottom()}.
 * These methods return the coordinates of the right and bottom edges of the
 * rectangle representing the view. For instance, calling {@link #getRight()}
 * is similar to the following computation: <code>getLeft() + getWidth()</code>
 * (see <a href="#SizePaddingMargins">Size</a> for more information about the width.)
 * </p>
 *
 * <a name="SizePaddingMargins"></a>
 * <h3>Size, padding and margins</h3>
 * <p>
 * The size of a view is expressed with a width and a height. A view actually
 * possess two pairs of width and height values.
 * </p>
 *
 * <p>
 * The first pair is known as <em>measured width</em> and
 * <em>measured height</em>. These dimensions define how big a view wants to be
 * within its parent (see <a href="#Layout">Layout</a> for more details.) The
 * measured dimensions can be obtained by calling {@link #getMeasuredWidth()}
 * and {@link #getMeasuredHeight()}.
 * </p>
 *
 * <p>
 * The second pair is simply known as <em>width</em> and <em>height</em>, or
 * sometimes <em>drawing width</em> and <em>drawing height</em>. These
 * dimensions define the actual size of the view on screen, at drawing time and
 * after layout. These values may, but do not have to, be different from the
 * measured width and height. The width and height can be obtained by calling
 * {@link #getWidth()} and {@link #getHeight()}.
 * </p>
 *
 * <p>
 * To measure its dimensions, a view takes into account its padding. The padding
 * is expressed in pixels for the left, top, right and bottom parts of the view.
 * Padding can be used to offset the content of the view by a specific amount of
 * pixels. For instance, a left padding of 2 will push the view's content by
 * 2 pixels to the right of the left edge. Padding can be set using the
 * {@link #setPadding(int, int, int, int)} or {@link #setPaddingRelative(int, int, int, int)}
 * method and queried by calling {@link #getPaddingLeft()}, {@link #getPaddingTop()},
 * {@link #getPaddingRight()}, {@link #getPaddingBottom()}, {@link #getPaddingStart()},
 * {@link #getPaddingEnd()}.
 * </p>
 *
 * <p>
 * Even though a view can define a padding, it does not provide any support for
 * margins. However, view groups provide such a support. Refer to
 * {@link android.view.ViewGroup} and
 * {@link android.view.ViewGroup.MarginLayoutParams} for further information.
 * </p>
 *
 * <a name="Layout"></a>
 * <h3>Layout</h3>
 * <p>
 * Layout is a two pass process: a measure pass and a layout pass. The measuring
 * pass is implemented in {@link #measure(int, int)} and is a top-down traversal
 * of the view tree. Each view pushes dimension specifications down the tree
 * during the recursion. At the end of the measure pass, every view has stored
 * its measurements. The second pass happens in
 * {@link #layout(int,int,int,int)} and is also top-down. During
 * this pass each parent is responsible for positioning all of its children
 * using the sizes computed in the measure pass.
 * </p>
 *
 * <p>
 * When a view's measure() method returns, its {@link #getMeasuredWidth()} and
 * {@link #getMeasuredHeight()} values must be set, along with those for all of
 * that view's descendants. A view's measured width and measured height values
 * must respect the constraints imposed by the view's parents. This guarantees
 * that at the end of the measure pass, all parents accept all of their
 * children's measurements. A parent view may call measure() more than once on
 * its children. For example, the parent may measure each child once with
 * unspecified dimensions to find out how big they want to be, then call
 * measure() on them again with actual numbers if the sum of all the children's
 * unconstrained sizes is too big or too small.
 * </p>
 *
 * <p>
 * The measure pass uses two classes to communicate dimensions. The
 * {@link MeasureSpec} class is used by views to tell their parents how they
 * want to be measured and positioned. The base LayoutParams class just
 * describes how big the view wants to be for both width and height. For each
 * dimension, it can specify one of:
 * <ul>
 * <li> an exact number
 * <li>MATCH_PARENT, which means the view wants to be as big as its parent
 * (minus padding)
 * <li> WRAP_CONTENT, which means that the view wants to be just big enough to
 * enclose its content (plus padding).
 * </ul>
 * There are subclasses of LayoutParams for different subclasses of ViewGroup.
 * For example, AbsoluteLayout has its own subclass of LayoutParams which adds
 * an X and Y value.
 * </p>
 *
 * <p>
 * MeasureSpecs are used to push requirements down the tree from parent to
 * child. A MeasureSpec can be in one of three modes:
 * <ul>
 * <li>UNSPECIFIED: This is used by a parent to determine the desired dimension
 * of a child view. For example, a LinearLayout may call measure() on its child
 * with the height set to UNSPECIFIED and a width of EXACTLY 240 to find out how
 * tall the child view wants to be given a width of 240 pixels.
 * <li>EXACTLY: This is used by the parent to impose an exact size on the
 * child. The child must use this size, and guarantee that all of its
 * descendants will fit within this size.
 * <li>AT_MOST: This is used by the parent to impose a maximum size on the
 * child. The child must guarantee that it and all of its descendants will fit
 * within this size.
 * </ul>
 * </p>
 *
 * <p>
 * To initiate a layout, call {@link #requestLayout}. This method is typically
 * called by a view on itself when it believes that it can no longer fit within
 * its current bounds.
 * </p>
 *
 * <a name="Drawing"></a>
 * <h3>Drawing</h3>
 * <p>
 * Drawing is handled by walking the tree and recording the drawing commands of
 * any View that needs to update. After this, the drawing commands of the
 * entire tree are issued to screen, clipped to the newly damaged area.
 * </p>
 *
 * <p>
 * The tree is largely recorded and drawn in order, with parents drawn before
 * (i.e., behind) their children, with siblings drawn in the order they appear
 * in the tree. If you set a background drawable for a View, then the View will
 * draw it before calling back to its <code>onDraw()</code> method. The child
 * drawing order can be overridden with
 * {@link ViewGroup#setChildrenDrawingOrderEnabled(boolean) custom child drawing order}
 * in a ViewGroup, and with {@link #setZ(float)} custom Z values} set on Views.
 * </p>
 *
 * <p>
 * To force a view to draw, call {@link #invalidate()}.
 * </p>
 *
 * <a name="EventHandlingThreading"></a>
 * <h3>Event Handling and Threading</h3>
 * <p>
 * The basic cycle of a view is as follows:
 * <ol>
 * <li>An event comes in and is dispatched to the appropriate view. The view
 * handles the event and notifies any listeners.</li>
 * <li>If in the course of processing the event, the view's bounds may need
 * to be changed, the view will call {@link #requestLayout()}.</li>
 * <li>Similarly, if in the course of processing the event the view's appearance
 * may need to be changed, the view will call {@link #invalidate()}.</li>
 * <li>If either {@link #requestLayout()} or {@link #invalidate()} were called,
 * the framework will take care of measuring, laying out, and drawing the tree
 * as appropriate.</li>
 * </ol>
 * </p>
 *
 * <p><em>Note: The entire view tree is single threaded. You must always be on
 * the UI thread when calling any method on any view.</em>
 * If you are doing work on other threads and want to update the state of a view
 * from that thread, you should use a {@link Handler}.
 * </p>
 *
 * <a name="FocusHandling"></a>
 * <h3>Focus Handling</h3>
 * <p>
 * The framework will handle routine focus movement in response to user input.
 * This includes changing the focus as views are removed or hidden, or as new
 * views become available. Views indicate their willingness to take focus
 * through the {@link #isFocusable} method. To change whether a view can take
 * focus, call {@link #setFocusable(boolean)}.  When in touch mode (see notes below)
 * views indicate whether they still would like focus via {@link #isFocusableInTouchMode}
 * and can change this via {@link #setFocusableInTouchMode(boolean)}.
 * </p>
 * <p>
 * Focus movement is based on an algorithm which finds the nearest neighbor in a
 * given direction. In rare cases, the default algorithm may not match the
 * intended behavior of the developer. In these situations, you can provide
 * explicit overrides by using these XML attributes in the layout file:
 * <pre>
 * nextFocusDown
 * nextFocusLeft
 * nextFocusRight
 * nextFocusUp
 * </pre>
 * </p>
 *
 *
 * <p>
 * To get a particular view to take focus, call {@link #requestFocus()}.
 * </p>
 *
 * <a name="TouchMode"></a>
 * <h3>Touch Mode</h3>
 * <p>
 * When a user is navigating a user interface via directional keys such as a D-pad, it is
 * necessary to give focus to actionable items such as buttons so the user can see
 * what will take input.  If the device has touch capabilities, however, and the user
 * begins interacting with the interface by touching it, it is no longer necessary to
 * always highlight, or give focus to, a particular view.  This motivates a mode
 * for interaction named 'touch mode'.
 * </p>
 * <p>
 * For a touch capable device, once the user touches the screen, the device
 * will enter touch mode.  From this point onward, only views for which
 * {@link #isFocusableInTouchMode} is true will be focusable, such as text editing widgets.
 * Other views that are touchable, like buttons, will not take focus when touched; they will
 * only fire the on click listeners.
 * </p>
 * <p>
 * Any time a user hits a directional key, such as a D-pad direction, the view device will
 * exit touch mode, and find a view to take focus, so that the user may resume interacting
 * with the user interface without touching the screen again.
 * </p>
 * <p>
 * The touch mode state is maintained across {@link android.app.Activity}s.  Call
 * {@link #isInTouchMode} to see whether the device is currently in touch mode.
 * </p>
 *
 * <a name="Scrolling"></a>
 * <h3>Scrolling</h3>
 * <p>
 * The framework provides basic support for views that wish to internally
 * scroll their content. This includes keeping track of the X and Y scroll
 * offset as well as mechanisms for drawing scrollbars. See
 * {@link #scrollBy(int, int)}, {@link #scrollTo(int, int)}, and
 * {@link #awakenScrollBars()} for more details.
 * </p>
 *
 * <a name="Tags"></a>
 * <h3>Tags</h3>
 * <p>
 * Unlike IDs, tags are not used to identify views. Tags are essentially an
 * extra piece of information that can be associated with a view. They are most
 * often used as a convenience to store data related to views in the views
 * themselves rather than by putting them in a separate structure.
 * </p>
 * <p>
 * Tags may be specified with character sequence values in layout XML as either
 * a single tag using the {@link android.R.styleable#View_tag android:tag}
 * attribute or multiple tags using the {@code <tag>} child element:
 * <pre>
 *     &lt;View ...
 *           android:tag="@string/mytag_value" /&gt;
 *     &lt;View ...&gt;
 *         &lt;tag android:id="@+id/mytag"
 *              android:value="@string/mytag_value" /&gt;
 *     &lt;/View>
 * </pre>
 * </p>
 * <p>
 * Tags may also be specified with arbitrary objects from code using
 * {@link #setTag(Object)} or {@link #setTag(int, Object)}.
 * </p>
 *
 * <a name="Themes"></a>
 * <h3>Themes</h3>
 * <p>
 * By default, Views are created using the theme of the Context object supplied
 * to their constructor; however, a different theme may be specified by using
 * the {@link android.R.styleable#View_theme android:theme} attribute in layout
 * XML or by passing a {@link ContextThemeWrapper} to the constructor from
 * code.
 * </p>
 * <p>
 * When the {@link android.R.styleable#View_theme android:theme} attribute is
 * used in XML, the specified theme is applied on top of the inflation
 * context's theme (see {@link LayoutInflater}) and used for the view itself as
 * well as any child elements.
 * </p>
 * <p>
 * In the following example, both views will be created using the Material dark
 * color scheme; however, because an overlay theme is used which only defines a
 * subset of attributes, the value of
 * {@link android.R.styleable#Theme_colorAccent android:colorAccent} defined on
 * the inflation context's theme (e.g. the Activity theme) will be preserved.
 * <pre>
 *     &lt;LinearLayout
 *             ...
 *             android:theme="@android:theme/ThemeOverlay.Material.Dark"&gt;
 *         &lt;View ...&gt;
 *     &lt;/LinearLayout&gt;
 * </pre>
 * </p>
 *
 * <a name="Properties"></a>
 * <h3>Properties</h3>
 * <p>
 * The View class exposes an {@link #ALPHA} property, as well as several transform-related
 * properties, such as {@link #TRANSLATION_X} and {@link #TRANSLATION_Y}. These properties are
 * available both in the {@link Property} form as well as in similarly-named setter/getter
 * methods (such as {@link #setAlpha(float)} for {@link #ALPHA}). These properties can
 * be used to set persistent state associated with these rendering-related properties on the view.
 * The properties and methods can also be used in conjunction with
 * {@link android.animation.Animator Animator}-based animations, described more in the
 * <a href="#Animation">Animation</a> section.
 * </p>
 *
 * <a name="Animation"></a>
 * <h3>Animation</h3>
 * <p>
 * Starting with Android 3.0, the preferred way of animating views is to use the
 * {@link android.animation} package APIs. These {@link android.animation.Animator Animator}-based
 * classes change actual properties of the View object, such as {@link #setAlpha(float) alpha} and
 * {@link #setTranslationX(float) translationX}. This behavior is contrasted to that of the pre-3.0
 * {@link android.view.animation.Animation Animation}-based classes, which instead animate only
 * how the view is drawn on the display. In particular, the {@link ViewPropertyAnimator} class
 * makes animating these View properties particularly easy and efficient.
 * </p>
 * <p>
 * Alternatively, you can use the pre-3.0 animation classes to animate how Views are rendered.
 * You can attach an {@link Animation} object to a view using
 * {@link #setAnimation(Animation)} or
 * {@link #startAnimation(Animation)}. The animation can alter the scale,
 * rotation, translation and alpha of a view over time. If the animation is
 * attached to a view that has children, the animation will affect the entire
 * subtree rooted by that node. When an animation is started, the framework will
 * take care of redrawing the appropriate views until the animation completes.
 * </p>
 *
 * <a name="Security"></a>
 * <h3>Security</h3>
 * <p>
 * Sometimes it is essential that an application be able to verify that an action
 * is being performed with the full knowledge and consent of the user, such as
 * granting a permission request, making a purchase or clicking on an advertisement.
 * Unfortunately, a malicious application could try to spoof the user into
 * performing these actions, unaware, by concealing the intended purpose of the view.
 * As a remedy, the framework offers a touch filtering mechanism that can be used to
 * improve the security of views that provide access to sensitive functionality.
 * </p><p>
 * To enable touch filtering, call {@link #setFilterTouchesWhenObscured(boolean)} or set the
 * android:filterTouchesWhenObscured layout attribute to true.  When enabled, the framework
 * will discard touches that are received whenever the view's window is obscured by
 * another visible window at the touched location.  As a result, the view will not receive touches
 * whenever the touch passed through a toast, dialog or other window that appears above the view's
 * window.
 * </p><p>
 * For more fine-grained control over security, consider overriding the
 * {@link #onFilterTouchEventForSecurity(MotionEvent)} method to implement your own
 * security policy. See also {@link MotionEvent#FLAG_WINDOW_IS_OBSCURED}.
 * </p>
 *
 * @attr ref android.R.styleable#View_accessibilityHeading
 * @attr ref android.R.styleable#View_allowClickWhenDisabled
 * @attr ref android.R.styleable#View_alpha
 * @attr ref android.R.styleable#View_background
 * @attr ref android.R.styleable#View_clickable
 * @attr ref android.R.styleable#View_clipToOutline
 * @attr ref android.R.styleable#View_contentDescription
 * @attr ref android.R.styleable#View_drawingCacheQuality
 * @attr ref android.R.styleable#View_duplicateParentState
 * @attr ref android.R.styleable#View_id
 * @attr ref android.R.styleable#View_requiresFadingEdge
 * @attr ref android.R.styleable#View_fadeScrollbars
 * @attr ref android.R.styleable#View_fadingEdgeLength
 * @attr ref android.R.styleable#View_filterTouchesWhenObscured
 * @attr ref android.R.styleable#View_fitsSystemWindows
 * @attr ref android.R.styleable#View_isScrollContainer
 * @attr ref android.R.styleable#View_focusable
 * @attr ref android.R.styleable#View_focusableInTouchMode
 * @attr ref android.R.styleable#View_focusedByDefault
 * @attr ref android.R.styleable#View_hapticFeedbackEnabled
 * @attr ref android.R.styleable#View_keepScreenOn
 * @attr ref android.R.styleable#View_keyboardNavigationCluster
 * @attr ref android.R.styleable#View_layerType
 * @attr ref android.R.styleable#View_layoutDirection
 * @attr ref android.R.styleable#View_longClickable
 * @attr ref android.R.styleable#View_minHeight
 * @attr ref android.R.styleable#View_minWidth
 * @attr ref android.R.styleable#View_nextClusterForward
 * @attr ref android.R.styleable#View_nextFocusDown
 * @attr ref android.R.styleable#View_nextFocusLeft
 * @attr ref android.R.styleable#View_nextFocusRight
 * @attr ref android.R.styleable#View_nextFocusUp
 * @attr ref android.R.styleable#View_onClick
 * @attr ref android.R.styleable#View_outlineSpotShadowColor
 * @attr ref android.R.styleable#View_outlineAmbientShadowColor
 * @attr ref android.R.styleable#View_padding
 * @attr ref android.R.styleable#View_paddingHorizontal
 * @attr ref android.R.styleable#View_paddingVertical
 * @attr ref android.R.styleable#View_paddingBottom
 * @attr ref android.R.styleable#View_paddingLeft
 * @attr ref android.R.styleable#View_paddingRight
 * @attr ref android.R.styleable#View_paddingTop
 * @attr ref android.R.styleable#View_paddingStart
 * @attr ref android.R.styleable#View_paddingEnd
 * @attr ref android.R.styleable#View_saveEnabled
 * @attr ref android.R.styleable#View_rotation
 * @attr ref android.R.styleable#View_rotationX
 * @attr ref android.R.styleable#View_rotationY
 * @attr ref android.R.styleable#View_scaleX
 * @attr ref android.R.styleable#View_scaleY
 * @attr ref android.R.styleable#View_scrollX
 * @attr ref android.R.styleable#View_scrollY
 * @attr ref android.R.styleable#View_scrollbarSize
 * @attr ref android.R.styleable#View_scrollbarStyle
 * @attr ref android.R.styleable#View_scrollbars
 * @attr ref android.R.styleable#View_scrollbarDefaultDelayBeforeFade
 * @attr ref android.R.styleable#View_scrollbarFadeDuration
 * @attr ref android.R.styleable#View_scrollbarTrackHorizontal
 * @attr ref android.R.styleable#View_scrollbarThumbHorizontal
 * @attr ref android.R.styleable#View_scrollbarThumbVertical
 * @attr ref android.R.styleable#View_scrollbarTrackVertical
 * @attr ref android.R.styleable#View_scrollbarAlwaysDrawHorizontalTrack
 * @attr ref android.R.styleable#View_scrollbarAlwaysDrawVerticalTrack
 * @attr ref android.R.styleable#View_stateListAnimator
 * @attr ref android.R.styleable#View_transitionName
 * @attr ref android.R.styleable#View_soundEffectsEnabled
 * @attr ref android.R.styleable#View_tag
 * @attr ref android.R.styleable#View_textAlignment
 * @attr ref android.R.styleable#View_textDirection
 * @attr ref android.R.styleable#View_transformPivotX
 * @attr ref android.R.styleable#View_transformPivotY
 * @attr ref android.R.styleable#View_translationX
 * @attr ref android.R.styleable#View_translationY
 * @attr ref android.R.styleable#View_translationZ
 * @attr ref android.R.styleable#View_visibility
 * @attr ref android.R.styleable#View_theme
 *
 * @see android.view.ViewGroup
 */
@UiThread
public class View implements Drawable.Callback, KeyEvent.Callback,
        AccessibilityEventSource {
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private static final boolean DBG = false;

    /** @hide */
    public static boolean DEBUG_DRAW = false;

    /**
     * The logging tag used by this class with android.util.Log.
     */
    protected static final String VIEW_LOG_TAG = "View";

    /**
     * The logging tag used by this class when logging verbose, autofill-related messages.
     */
    // NOTE: We cannot use android.view.autofill.Helper.sVerbose because that variable is not
    // set if a session is not started.
    private static final String AUTOFILL_LOG_TAG = "View.Autofill";

    /**
     * The logging tag used by this class when logging content capture-related messages.
     */
    private static final String CONTENT_CAPTURE_LOG_TAG = "View.ContentCapture";

    private static final boolean DEBUG_CONTENT_CAPTURE = false;

    /**
     * When set to true, this view will save its attribute data.
     *
     * @hide
     */
    public static boolean sDebugViewAttributes = false;

    /**
     * When set to this application package view will save its attribute data.
     *
     * @hide
     */
    public static String sDebugViewAttributesApplicationPackage;

    /**
     * Used to mark a View that has no ID.
     */
    public static final int NO_ID = -1;

    /**
     * Last ID that is given to Views that are no part of activities.
     *
     * {@hide}
     */
    public static final int LAST_APP_AUTOFILL_ID = Integer.MAX_VALUE / 2;

    /**
     * Attribute to find the autofilled highlight
     *
     * @see #getAutofilledDrawable()
     */
    private static final int[] AUTOFILL_HIGHLIGHT_ATTR =
            new int[]{android.R.attr.autofilledHighlight};

    /**
     * Signals that compatibility booleans have been initialized according to
     * target SDK versions.
     */
    private static boolean sCompatibilityDone = false;

    /** @hide */
    public HapticScrollFeedbackProvider mScrollFeedbackProvider = null;

    /**
     * Ignore an optimization that skips unnecessary EXACTLY layout passes.
     */
    private static boolean sAlwaysRemeasureExactly = false;

    /**
     * When true calculates the bounds in parent from bounds in screen relative to its parents.
     * This addresses the deprecated API (setBoundsInParent) in Compose, which causes empty
     * getBoundsInParent call for Compose apps.
     */
    private static boolean sCalculateBoundsInParentFromBoundsInScreenFlagValue = false;

    /**
     * When true makes it possible to use onMeasure caches also when the force layout flag is
     * enabled. This helps avoiding multiple measures in the same frame with the same dimensions.
     */
    private static boolean sUseMeasureCacheDuringForceLayoutFlagValue;

    /**
     * Allow setForeground/setBackground to be called (and ignored) on a textureview,
     * without throwing
     */
    static boolean sTextureViewIgnoresDrawableSetters = false;

    /**
     * Prior to N, some ViewGroups would not convert LayoutParams properly even though both extend
     * MarginLayoutParams. For instance, converting LinearLayout.LayoutParams to
     * RelativeLayout.LayoutParams would lose margin information. This is fixed on N but target API
     * check is implemented for backwards compatibility.
     *
     * {@hide}
     */
    protected static boolean sPreserveMarginParamsInLayoutParamConversion;

    /**
     * Prior to N, when drag enters into child of a view that has already received an
     * ACTION_DRAG_ENTERED event, the parent doesn't get a ACTION_DRAG_EXITED event.
     * ACTION_DRAG_LOCATION and ACTION_DROP were delivered to the parent of a view that returned
     * false from its event handler for these events.
     * Starting from N, the parent will get ACTION_DRAG_EXITED event before the child gets its
     * ACTION_DRAG_ENTERED. ACTION_DRAG_LOCATION and ACTION_DROP are never propagated to the parent.
     * sCascadedDragDrop is true for pre-N apps for backwards compatibility implementation.
     */
    static boolean sCascadedDragDrop;

    /**
     * Prior to O, auto-focusable didn't exist and widgets such as ListView use hasFocusable
     * to determine things like whether or not to permit item click events. We can't break
     * apps that do this just because more things (clickable things) are now auto-focusable
     * and they would get different results, so give old behavior to old apps.
     */
    static boolean sHasFocusableExcludeAutoFocusable;

    /**
     * Prior to O, auto-focusable didn't exist and views marked as clickable weren't implicitly
     * made focusable by default. As a result, apps could (incorrectly) change the clickable
     * setting of views off the UI thread. Now that clickable can effect the focusable state,
     * changing the clickable attribute off the UI thread will cause an exception (since changing
     * the focusable state checks). In order to prevent apps from crashing, we will handle this
     * specific case and just not notify parents on new focusables resulting from marking views
     * clickable from outside the UI thread.
     */
    private static boolean sAutoFocusableOffUIThreadWontNotifyParents;

    /**
     * Prior to P things like setScaleX() allowed passing float values that were bogus such as
     * Float.NaN. If the app is targetting P or later then passing these values will result in an
     * exception being thrown. If the app is targetting an earlier SDK version, then we will
     * silently clamp these values to avoid crashes elsewhere when the rendering code hits
     * these bogus values.
     */
    private static boolean sThrowOnInvalidFloatProperties;

    /**
     * Prior to P, {@code #startDragAndDrop} accepts a builder which produces an empty drag shadow.
     * Currently zero size SurfaceControl cannot be created thus we create a 1x1 surface instead.
     */
    private static boolean sAcceptZeroSizeDragShadow;

    /**
     * When true, measure and layout passes of all the newly attached views will be logged with
     * {@link Trace}, so we can better debug jank due to complex view hierarchies.
     */
    private static boolean sTraceLayoutSteps;

    /**
     * When not null, emits a {@link Trace} instant event and the stacktrace every time a relayout
     * of a class having this name happens.
     */
    private static String sTraceRequestLayoutClass;

    @Nullable
    private ViewCredentialHandler mViewCredentialHandler;

    /** Used to avoid computing the full strings each time when layout tracing is enabled. */
    @Nullable
    private ViewTraversalTracingStrings mTracingStrings;

    /**
     * Prior to R, {@link #dispatchApplyWindowInsets} had an issue:
     * <p>The modified insets changed by {@link #onApplyWindowInsets} were passed to the
     * entire view hierarchy in prefix order, including siblings as well as siblings of parents
     * further down the hierarchy. This violates the basic concepts of the view hierarchy, and
     * thus, the hierarchical dispatching mechanism was hard to use for apps.
     * <p>
     * In order to make window inset dispatching work properly, we dispatch window insets
     * in the view hierarchy in a proper hierarchical manner if this flag is set to {@code false}.
     */
    static boolean sBrokenInsetsDispatch;

    /**
     * Prior to Q, calling
     * {@link com.android.internal.policy.DecorView#setBackgroundDrawable(Drawable)}
     * did not call update the window format so the opacity of the background was not correctly
     * applied to the window. Some applications rely on this misbehavior to work properly.
     * <p>
     * From Q, {@link com.android.internal.policy.DecorView#setBackgroundDrawable(Drawable)} is
     * the same as {@link com.android.internal.policy.DecorView#setWindowBackground(Drawable)}
     * which updates the window format.
     * @hide
     */
    protected static boolean sBrokenWindowBackground;

    /**
     * Prior to R, we were always forcing a layout of the entire hierarchy when insets changed from
     * the server. This is inefficient and not all apps use it. Instead, we want to rely on apps
     * calling {@link #requestLayout} when they need to relayout based on an insets change.
     */
    static boolean sForceLayoutWhenInsetsChanged;

    /** @hide */
    @IntDef({NOT_FOCUSABLE, FOCUSABLE, FOCUSABLE_AUTO})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Focusable {}

    /**
     * This view does not want keystrokes.
     * <p>
     * Use with {@link #setFocusable(int)} and <a href="#attr_android:focusable">{@code
     * android:focusable}.
     */
    public static final int NOT_FOCUSABLE = 0x00000000;

    /**
     * This view wants keystrokes.
     * <p>
     * Use with {@link #setFocusable(int)} and <a href="#attr_android:focusable">{@code
     * android:focusable}.
     */
    public static final int FOCUSABLE = 0x00000001;

    /**
     * This view determines focusability automatically. This is the default.
     * <p>
     * Use with {@link #setFocusable(int)} and <a href="#attr_android:focusable">{@code
     * android:focusable}.
     */
    public static final int FOCUSABLE_AUTO = 0x00000010;

    /**
     * Mask for use with setFlags indicating bits used for focus.
     */
    private static final int FOCUSABLE_MASK = 0x00000011;

    /**
     * This view will adjust its padding to fit system windows (e.g. status bar)
     */
    private static final int FITS_SYSTEM_WINDOWS = 0x00000002;

    /** @hide */
    @IntDef({VISIBLE, INVISIBLE, GONE})
    @Retention(RetentionPolicy.SOURCE)
    public @interface Visibility {}

    /**
     * This view is visible.
     * Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int VISIBLE = 0x00000000;

    /**
     * This view is invisible, but it still takes up space for layout purposes.
     * Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int INVISIBLE = 0x00000004;

    /**
     * This view is invisible, and it doesn't take any space for layout
     * purposes. Use with {@link #setVisibility} and <a href="#attr_android:visibility">{@code
     * android:visibility}.
     */
    public static final int GONE = 0x00000008;

    /**
     * Mask for use with setFlags indicating bits used for visibility.
     * {@hide}
     */
    static final int VISIBILITY_MASK = 0x0000000C;

    private static final int[] VISIBILITY_FLAGS = {VISIBLE, INVISIBLE, GONE};

    /**
     * Hint indicating that this view can be autofilled with an email address.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_EMAIL_ADDRESS}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_EMAIL_ADDRESS = "emailAddress";

    /**
     * Hint indicating that this view can be autofilled with a user's real name.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_NAME}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_NAME = "name";

    /**
     * Hint indicating that this view can be autofilled with a username.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_USERNAME}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_USERNAME = "username";

    /**
     * Hint indicating that this view can be autofilled with a password.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_PASSWORD}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_PASSWORD = "password";

    /**
     * Hint indicating that this view can be autofilled with a phone number.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_PHONE}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_PHONE = "phone";

    /**
     * Hint indicating that this view can be autofilled with a postal address.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_POSTAL_ADDRESS}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_POSTAL_ADDRESS = "postalAddress";

    /**
     * Hint indicating that this view can be autofilled with a postal code.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_POSTAL_CODE}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_POSTAL_CODE = "postalCode";

    /**
     * Hint indicating that this view can be autofilled with a credit card number.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_NUMBER}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_NUMBER = "creditCardNumber";

    /**
     * Hint indicating that this view can be autofilled with a credit card security code.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE = "creditCardSecurityCode";

    /**
     * Hint indicating that this view can be autofilled with a credit card expiration date.
     *
     * <p>It should be used when the credit card expiration date is represented by just one view;
     * if it is represented by more than one (for example, one view for the month and another view
     * for the year), then each of these views should use the hint specific for the unit
     * ({@link #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY},
     * {@link #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH},
     * or {@link #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR}).
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE}</code>).
     *
     * <p>When annotating a view with this hint, it's recommended to use a date autofill value to
     * avoid ambiguity when the autofill service provides a value for it. To understand why a
     * value can be ambiguous, consider "April of 2020", which could be represented as either of
     * the following options:
     *
     * <ul>
     *   <li>{@code "04/2020"}
     *   <li>{@code "4/2020"}
     *   <li>{@code "2020/04"}
     *   <li>{@code "2020/4"}
     *   <li>{@code "April/2020"}
     *   <li>{@code "Apr/2020"}
     * </ul>
     *
     * <p>You define a date autofill value for the view by overriding the following methods:
     *
     * <ol>
     *   <li>{@link #getAutofillType()} to return {@link #AUTOFILL_TYPE_DATE}.
     *   <li>{@link #getAutofillValue()} to return a
     *       {@link AutofillValue#forDate(long) date autofillvalue}.
     *   <li>{@link #autofill(AutofillValue)} to expect a data autofillvalue.
     * </ol>
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE =
            "creditCardExpirationDate";

    /**
     * Hint indicating that this view can be autofilled with a credit card expiration month.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH}</code>).
     *
     * <p>When annotating a view with this hint, it's recommended to use a text autofill value
     * whose value is the numerical representation of the month, starting on {@code 1} to avoid
     * ambiguity when the autofill service provides a value for it. To understand why a
     * value can be ambiguous, consider "January", which could be represented as either of
     *
     * <ul>
     *   <li>{@code "1"}: recommended way.
     *   <li>{@code "0"}: if following the {@link Calendar#MONTH} convention.
     *   <li>{@code "January"}: full name, in English.
     *   <li>{@code "jan"}: abbreviated name, in English.
     *   <li>{@code "Janeiro"}: full name, in another language.
     * </ul>
     *
     * <p>Another recommended approach is to use a date autofill value - see
     * {@link #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE} for more details.
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH =
            "creditCardExpirationMonth";

    /**
     * Hint indicating that this view can be autofilled with a credit card expiration year.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR =
            "creditCardExpirationYear";

    /**
     * Hint indicating that this view can be autofilled with a credit card expiration day.
     *
     * <p>Can be used with either {@link #setAutofillHints(String[])} or
     * <a href="#attr_android:autofillHint"> {@code android:autofillHint}</a> (in which case the
     * value should be <code>{@value #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY}</code>).
     *
     * <p>See {@link #setAutofillHints(String...)} for more info about autofill hints.
     */
    public static final String AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY = "creditCardExpirationDay";

    /**
     * A hint indicating that this view can be autofilled with a password.
     *
     * This is a heuristic-based hint that is meant to be used by UI Toolkit developers when a
     * view is a password field but doesn't specify a
     * <code>{@value View#AUTOFILL_HINT_PASSWORD}</code>.
     * @hide
     */
    // TODO(229765029): unhide this for UI toolkit
    public static final String AUTOFILL_HINT_PASSWORD_AUTO = "passwordAuto";

    /**
     * Hint indicating that the developer intends to fill this view with output from
     * CredentialManager.
     *
     * @hide
     */
    public static final String AUTOFILL_HINT_CREDENTIAL_MANAGER = "credential";

    /**
     * Hints for the autofill services that describes the content of the view.
     */
    private @Nullable String[] mAutofillHints;

    /**
     * Autofill id, lazily created on calls to {@link #getAutofillId()}.
     */
    private AutofillId mAutofillId;

    /** @hide */
    @IntDef(prefix = { "AUTOFILL_TYPE_" }, value = {
            AUTOFILL_TYPE_NONE,
            AUTOFILL_TYPE_TEXT,
            AUTOFILL_TYPE_TOGGLE,
            AUTOFILL_TYPE_LIST,
            AUTOFILL_TYPE_DATE,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AutofillType {}

    /**
     * Autofill type for views that cannot be autofilled.
     *
     * <p>Typically used when the view is read-only; for example, a text label.
     *
     * @see #getAutofillType()
     */
    public static final int AUTOFILL_TYPE_NONE = 0;

    /**
     * Autofill type for a text field, which is filled by a {@link CharSequence}.
     *
     * <p>{@link AutofillValue} instances for autofilling a {@link View} can be obtained through
     * {@link AutofillValue#forText(CharSequence)}, and the value passed to autofill a
     * {@link View} can be fetched through {@link AutofillValue#getTextValue()}.
     *
     * @see #getAutofillType()
     */
    public static final int AUTOFILL_TYPE_TEXT = 1;

    /**
     * Autofill type for a togglable field, which is filled by a {@code boolean}.
     *
     * <p>{@link AutofillValue} instances for autofilling a {@link View} can be obtained through
     * {@link AutofillValue#forToggle(boolean)}, and the value passed to autofill a
     * {@link View} can be fetched through {@link AutofillValue#getToggleValue()}.
     *
     * @see #getAutofillType()
     */
    public static final int AUTOFILL_TYPE_TOGGLE = 2;
  
    /**
     * Autofill type for a selection list field, which is filled by an {@code int}
     * representing the element index inside the list (starting at {@code 0}).
     *
     * <p>{@link AutofillValue} instances for autofilling a {@link View} can be obtained through
     * {@link AutofillValue#forList(int)}, and the value passed to autofill a
     * {@link View} can be fetched through {@link AutofillValue#getListValue()}.
     *
     * <p>The available options in the selection list are typically provided by
     * {@link android.app.assist.AssistStructure.ViewNode#getAutofillOptions()}.
     *
     * @see #getAutofillType()
     */
    public static final int AUTOFILL_TYPE_LIST = 3;

    /**
     * Autofill type for a field that contains a date, which is represented by a long representing
     * the number of milliseconds since the standard base time known as "the epoch", namely
     * January 1, 1970, 00:00:00 GMT (see {@link java.util.Date#getTime()}.
     *
     * <p>{@link AutofillValue} instances for autofilling a {@link View} can be obtained through
     * {@link AutofillValue#forDate(long)}, and the values passed to
     * autofill a {@link View} can be fetched through {@link AutofillValue#getDateValue()}.
     *
     * @see #getAutofillType()
     */
    public static final int AUTOFILL_TYPE_DATE = 4;


    /** @hide */
    @IntDef(prefix = { "IMPORTANT_FOR_AUTOFILL_" }, value = {
            IMPORTANT_FOR_AUTOFILL_AUTO,
            IMPORTANT_FOR_AUTOFILL_YES,
            IMPORTANT_FOR_AUTOFILL_NO,
            IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS,
            IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AutofillImportance {}

    /**
     * Automatically determine whether a view is important for autofill.
     *
     * @see #isImportantForAutofill()
     * @see #setImportantForAutofill(int)
     */
    public static final int IMPORTANT_FOR_AUTOFILL_AUTO = 0x0;

    /**
     * The view is important for autofill, and its children (if any) will be traversed.
     *
     * @see #isImportantForAutofill()
     * @see #setImportantForAutofill(int)
     */
    public static final int IMPORTANT_FOR_AUTOFILL_YES = 0x1;

    /**
     * The view is not important for autofill, but its children (if any) will be traversed.
     *
     * @see #isImportantForAutofill()
     * @see #setImportantForAutofill(int)
     */
    public static final int IMPORTANT_FOR_AUTOFILL_NO = 0x2;

    /**
     * The view is important for autofill, but its children (if any) will not be traversed.
     *
     * @see #isImportantForAutofill()
     * @see #setImportantForAutofill(int)
     */
    public static final int IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS = 0x4;

    /**
     * The view is not important for autofill, and its children (if any) will not be traversed.
     *
     * @see #isImportantForAutofill()
     * @see #setImportantForAutofill(int)
     */
    public static final int IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS = 0x8;

    /** @hide */
    @IntDef(flag = true, prefix = { "AUTOFILL_FLAG_" }, value = {
            AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AutofillFlags {}

    /**
     * Flag requesting you to add views that are marked as not important for autofill
     * (see {@link #setImportantForAutofill(int)}) to a {@link ViewStructure}.
     */
    public static final int AUTOFILL_FLAG_INCLUDE_NOT_IMPORTANT_VIEWS = 0x1;

    /** @hide */
    @IntDef(prefix = { "IMPORTANT_FOR_CONTENT_CAPTURE_" }, value = {
            IMPORTANT_FOR_CONTENT_CAPTURE_AUTO,
            IMPORTANT_FOR_CONTENT_CAPTURE_YES,
            IMPORTANT_FOR_CONTENT_CAPTURE_NO,
            IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS,
            IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentCaptureImportance {}
  

    /**
     * Automatically determine whether a view is important for content capture.
     *
     * @see #isImportantForContentCapture()
     * @see #setImportantForContentCapture(int)
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_AUTO = 0x0;

    /**
     * The view is important for content capture, and its children (if any) will be traversed.
     *
     * @see #isImportantForContentCapture()
     * @see #setImportantForContentCapture(int)
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES = 0x1;

    /**
     * The view is not important for content capture, but its children (if any) will be traversed.
     *
     * @see #isImportantForContentCapture()
     * @see #setImportantForContentCapture(int)
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO = 0x2;

    /**
     * The view is important for content capture, but its children (if any) will not be traversed.
     *
     * @see #isImportantForContentCapture()
     * @see #setImportantForContentCapture(int)
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS = 0x4;

    /**
     * The view is not important for content capture, and its children (if any) will not be
     * traversed.
     *
     * @see #isImportantForContentCapture()
     * @see #setImportantForContentCapture(int)
     */
    public static final int IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS = 0x8;

    /** {@hide} */
    @IntDef(flag = true, prefix = {"SCROLL_CAPTURE_HINT_"},
            value = {
                    SCROLL_CAPTURE_HINT_AUTO,
                    SCROLL_CAPTURE_HINT_EXCLUDE,
                    SCROLL_CAPTURE_HINT_INCLUDE,
                    SCROLL_CAPTURE_HINT_EXCLUDE_DESCENDANTS
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollCaptureHint {}

    /**
     * The content of this view will be considered for scroll capture if scrolling is possible.
     *
     * @see #getScrollCaptureHint()
     * @see #setScrollCaptureHint(int)
     */
    public static final int SCROLL_CAPTURE_HINT_AUTO = 0;

    /**
     * Explicitly exclude this view as a potential scroll capture target. The system will not
     * consider it. Mutually exclusive with {@link #SCROLL_CAPTURE_HINT_INCLUDE}, which this flag
     * takes precedence over.
     *
     * @see #getScrollCaptureHint()
     * @see #setScrollCaptureHint(int)
     */
    public static final int SCROLL_CAPTURE_HINT_EXCLUDE = 0x1;

    /**
     * Explicitly include this view as a potential scroll capture target. When locating a scroll
     * capture target, this view will be prioritized before others without this flag. Mutually
     * exclusive with {@link #SCROLL_CAPTURE_HINT_EXCLUDE}, which takes precedence.
     *
     * @see #getScrollCaptureHint()
     * @see #setScrollCaptureHint(int)
     */
    public static final int SCROLL_CAPTURE_HINT_INCLUDE = 0x2;

    /**
     * Explicitly exclude all children of this view as potential scroll capture targets. This view
     * is unaffected. Note: Excluded children are not considered, regardless of {@link
     * #SCROLL_CAPTURE_HINT_INCLUDE}.
     *
     * @see #getScrollCaptureHint()
     * @see #setScrollCaptureHint(int)
     */
    public static final int SCROLL_CAPTURE_HINT_EXCLUDE_DESCENDANTS = 0x4;

    /**
     * This view is enabled. Interpretation varies by subclass.
     * Use with ENABLED_MASK when calling setFlags.
     * {@hide}
     */
    static final int ENABLED = 0x00000000;

    /**
     * This view is disabled. Interpretation varies by subclass.
     * Use with ENABLED_MASK when calling setFlags.
     * {@hide}
     */
    static final int DISABLED = 0x00000020;

    /**
     * Mask for use with setFlags indicating bits used for indicating whether
     * this view is enabled
     * {@hide}
     */
    static final int ENABLED_MASK = 0x00000020;

    /**
     * This view won't draw. {@link #onDraw(android.graphics.Canvas)} won't be
     * called and further optimizations will be performed. It is okay to have
     * this flag set and a background. Use with DRAW_MASK when calling setFlags.
     * {@hide}
     */
    static final int WILL_NOT_DRAW = 0x00000080;

    /**
     * Mask for use with setFlags indicating bits used for indicating whether
     * this view is will draw
     * {@hide}
     */
    static final int DRAW_MASK = 0x00000080;

    /**
     * <p>This view doesn't show scrollbars.</p>
     * {@hide}
     */
    static final int SCROLLBARS_NONE = 0x00000000;

    /**
     * <p>This view shows horizontal scrollbars.</p>
     * {@hide}
     */
    static final int SCROLLBARS_HORIZONTAL = 0x00000100;

    /**
     * <p>This view shows vertical scrollbars.</p>
     * {@hide}
     */
    static final int SCROLLBARS_VERTICAL = 0x00000200;

    /**
     * <p>Mask for use with setFlags indicating bits used for indicating which
     * scrollbars are enabled.</p>
     * {@hide}
     */
    static final int SCROLLBARS_MASK = 0x00000300;

    /**
     * Indicates that the view should filter touches when its window is obscured.
     * Refer to the class comments for more information about this security feature.
     * {@hide}
     */
    static final int FILTER_TOUCHES_WHEN_OBSCURED = 0x00000400;

    /**
     * Set for framework elements that use FITS_SYSTEM_WINDOWS, to indicate
     * that they are optional and should be skipped if the window has
     * requested system UI flags that ignore those insets for layout.
     * <p>
     * This is only used for support library as of Android R. The framework now uses
     * {@link #PFLAG4_FRAMEWORK_OPTIONAL_FITS_SYSTEM_WINDOWS} such that it can skip the legacy
     * insets path that loses insets information.
     */
    static final int OPTIONAL_FITS_SYSTEM_WINDOWS = 0x00000800;

    /**
     * <p>This view doesn't show fading edges.</p>
     * {@hide}
     */
    static final int FADING_EDGE_NONE = 0x00000000;

    /**
     * <p>This view shows horizontal fading edges.</p>
     * {@hide}
     */
    static final int FADING_EDGE_HORIZONTAL = 0x00001000;

    /**
     * <p>This view shows vertical fading edges.</p>
     * {@hide}
     */
    static final int FADING_EDGE_VERTICAL = 0x00002000;

    /**
     * <p>Mask for use with setFlags indicating bits used for indicating which
     * fading edges are enabled.</p>
     * {@hide}
     */
    static final int FADING_EDGE_MASK = 0x00003000;

    /**
     * <p>Indicates this view can be clicked. When clickable, a View reacts
     * to clicks by notifying the OnClickListener.<p>
     * {@hide}
     */
    static final int CLICKABLE = 0x00004000;

    /**
     * <p>Indicates this view is caching its drawing into a bitmap.</p>
     * {@hide}
     */
    static final int DRAWING_CACHE_ENABLED = 0x00008000;

    /**
     * <p>Indicates that no icicle should be saved for this view.<p>
     * {@hide}
     */
    static final int SAVE_DISABLED = 0x000010000;

    /**
     * <p>Mask for use with setFlags indicating bits used for the saveEnabled
     * property.</p>
     * {@hide}
     */
    static final int SAVE_DISABLED_MASK = 0x000010000;

    /**
     * <p>Indicates that no drawing cache should ever be created for this view.<p>
     * {@hide}
     */
    static final int WILL_NOT_CACHE_DRAWING = 0x000020000;

    /**
     * <p>Indicates this view can take / keep focus when int touch mode.</p>
     * {@hide}
     */
    static final int FOCUSABLE_IN_TOUCH_MODE = 0x00040000;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(prefix = { "DRAWING_CACHE_QUALITY_" }, value = {
            DRAWING_CACHE_QUALITY_LOW,
            DRAWING_CACHE_QUALITY_HIGH,
            DRAWING_CACHE_QUALITY_AUTO
    })
    public @interface DrawingCacheQuality {}

    /**
     * <p>Enables low quality mode for the drawing cache.</p>
     *
     * @deprecated The view drawing cache was largely made obsolete with the introduction of
     * hardware-accelerated rendering in API 11. With hardware-acceleration, intermediate cache
     * layers are largely unnecessary and can easily result in a net loss in performance due to the
     * cost of creating and updating the layer. In the rare cases where caching layers are useful,
     * such as for alpha animations, {@link #setLayerType(int, Paint)} handles this with hardware
     * rendering. For software-rendered snapshots of a small part of the View hierarchy or
     * individual Views it is recommended to create a {@link Canvas} from either a {@link Bitmap} or
     * {@link android.graphics.Picture} and call {@link #draw(Canvas)} on the View. However these
     * software-rendered usages are discouraged and have compatibility issues with hardware-only
     * rendering features such as {@link android.graphics.Bitmap.Config#HARDWARE Config.HARDWARE}
     * bitmaps, real-time shadows, and outline clipping. For screenshots of the UI for feedback
     * reports or unit testing the {@link PixelCopy} API is recommended.
     */
    @Deprecated
    public static final int DRAWING_CACHE_QUALITY_LOW = 0x00080000;

    /**
     * <p>Enables high quality mode for the drawing cache.</p>
     *
     * @deprecated The view drawing cache was largely made obsolete with the introduction of
     * hardware-accelerated rendering in API 11. With hardware-acceleration, intermediate cache
     * layers are largely unnecessary and can easily result in a net loss in performance due to the
     * cost of creating and updating the layer. In the rare cases where caching layers are useful,
     * such as for alpha animations, {@link #setLayerType(int, Paint)} handles this with hardware
     * rendering. For software-rendered snapshots of a small part of the View hierarchy or
     * individual Views it is recommended to create a {@link Canvas} from either a {@link Bitmap} or
     * {@link android.graphics.Picture} and call {@link #draw(Canvas)} on the View. However these
     * software-rendered usages are discouraged and have compatibility issues with hardware-only
     * rendering features such as {@link android.graphics.Bitmap.Config#HARDWARE Config.HARDWARE}
     * bitmaps, real-time shadows, and outline clipping. For screenshots of the UI for feedback
     * reports or unit testing the {@link PixelCopy} API is recommended.
     */
    @Deprecated
    public static final int DRAWING_CACHE_QUALITY_HIGH = 0x00100000;

    /**
     * <p>Enables automatic quality mode for the drawing cache.</p>
     *
     * @deprecated The view drawing cache was largely made obsolete with the introduction of
     * hardware-accelerated rendering in API 11. With hardware-acceleration, intermediate cache
     * layers are largely unnecessary and can easily result in a net loss in performance due to the
     * cost of creating and updating the layer. In the rare cases where caching layers are useful,
     * such as for alpha animations, {@link #setLayerType(int, Paint)} handles this with hardware
     * rendering. For software-rendered snapshots of a small part of the View hierarchy or
     * individual Views it is recommended to create a {@link Canvas} from either a {@link Bitmap} or
     * {@link android.graphics.Picture} and call {@link #draw(Canvas)} on the View. However these
     * software-rendered usages are discouraged and have compatibility issues with hardware-only
     * rendering features such as {@link android.graphics.Bitmap.Config#HARDWARE Config.HARDWARE}
     * bitmaps, real-time shadows, and outline clipping. For screenshots of the UI for feedback
     * reports or unit testing the {@link PixelCopy} API is recommended.
     */
    @Deprecated
    public static final int DRAWING_CACHE_QUALITY_AUTO = 0x00000000;

    private static final int[] DRAWING_CACHE_QUALITY_FLAGS = {
            DRAWING_CACHE_QUALITY_AUTO, DRAWING_CACHE_QUALITY_LOW, DRAWING_CACHE_QUALITY_HIGH
    };

    /**
     * <p>Mask for use with setFlags indicating bits used for the cache
     * quality property.</p>
     * {@hide}
     */
    static final int DRAWING_CACHE_QUALITY_MASK = 0x00180000;

    /**
     * <p>
     * Indicates this view can be long clicked. When long clickable, a View
     * reacts to long clicks by notifying the OnLongClickListener or showing a
     * context menu.
     * </p>
     * {@hide}
     */
    static final int LONG_CLICKABLE = 0x00200000;

    /**
     * <p>Indicates that this view gets its drawable states from its direct parent
     * and ignores its original internal states.</p>
     *
     * @hide
     */
    static final int DUPLICATE_PARENT_STATE = 0x00400000;

    /**
     * <p>
     * Indicates this view can be context clicked. When context clickable, a View reacts to a
     * context click (e.g. a primary stylus button press or right mouse click) by notifying the
     * OnContextClickListener.
     * </p>
     * {@hide}
     */
    static final int CONTEXT_CLICKABLE = 0x00800000;

    /** @hide */
    @IntDef(prefix = { "SCROLLBARS_" }, value = {
            SCROLLBARS_INSIDE_OVERLAY,
            SCROLLBARS_INSIDE_INSET,
            SCROLLBARS_OUTSIDE_OVERLAY,
            SCROLLBARS_OUTSIDE_INSET
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ScrollBarStyle {}

    /**
     * The scrollbar style to display the scrollbars inside the content area,
     * without increasing the padding. The scrollbars will be overlaid with
     * translucency on the view's content.
     */
    public static final int SCROLLBARS_INSIDE_OVERLAY = 0;

    /**
     * The scrollbar style to display the scrollbars inside the padded area,
     * increasing the padding of the view. The scrollbars will not overlap the
     * content area of the view.
     */
    public static final int SCROLLBARS_INSIDE_INSET = 0x01000000;

    /**
     * The scrollbar style to display the scrollbars at the edge of the view,
     * without increasing the padding. The scrollbars will be overlaid with
     * translucency.
     */
    public static final int SCROLLBARS_OUTSIDE_OVERLAY = 0x02000000;

    /**
     * The scrollbar style to display the scrollbars at the edge of the view,
     * increasing the padding of the view. The scrollbars will only overlap the
     * background, if any.
     */
    public static final int SCROLLBARS_OUTSIDE_INSET = 0x03000000;

    /**
     * Mask to check if the scrollbar style is overlay or inset.
     * {@hide}
     */
    static final int SCROLLBARS_INSET_MASK = 0x01000000;

    /**
     * Mask to check if the scrollbar style is inside or outside.
     * {@hide}
     */
    static final int SCROLLBARS_OUTSIDE_MASK = 0x02000000;

    /**
     * Mask for scrollbar style.
     * {@hide}
     */
    static final int SCROLLBARS_STYLE_MASK = 0x03000000;

    /**
     * View flag indicating that the screen should remain on while the
     * window containing this view is visible to the user.  This effectively
     * takes care of automatically setting the WindowManager's
     * {@link WindowManager.LayoutParams#FLAG_KEEP_SCREEN_ON}.
     */
    public static final int KEEP_SCREEN_ON = 0x04000000;

    /**
     * View flag indicating whether this view should have sound effects enabled
     * for events such as clicking and touching.
     */
    public static final int SOUND_EFFECTS_ENABLED = 0x08000000;

    /**
     * View flag indicating whether this view should have haptic feedback
     * enabled for events such as long presses.
     */
    public static final int HAPTIC_FEEDBACK_ENABLED = 0x10000000;

    /**
     * <p>Indicates that the view hierarchy should stop saving state when
     * it reaches this view.  If state saving is initiated immediately at
     * the view, it will be allowed.
     * {@hide}
     */
    static final int PARENT_SAVE_DISABLED = 0x20000000;

    /**
     * <p>Mask for use with setFlags indicating bits used for PARENT_SAVE_DISABLED.</p>
     * {@hide}
     */
    static final int PARENT_SAVE_DISABLED_MASK = 0x20000000;

    private static Paint sDebugPaint;

    /**
     * <p>Indicates this view can display a tooltip on hover or long press.</p>
     * {@hide}
     */
    static final int TOOLTIP = 0x40000000;

    /** @hide */
    @IntDef(prefix = { "CONTENT_SENSITIVITY_" }, value = {
            CONTENT_SENSITIVITY_AUTO,
            CONTENT_SENSITIVITY_SENSITIVE,
            CONTENT_SENSITIVITY_NOT_SENSITIVE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ContentSensitivity {}

    /**
     * Content sensitivity is determined by the framework. The framework uses a heuristic to
     * determine if this view displays sensitive content.
     * Autofill hints i.e. {@link #getAutofillHints()}  are used in the heuristic
     * to determine if this view should be considered as a sensitive view.
     * <p>
     * {@link #AUTOFILL_HINT_USERNAME},
     * {@link #AUTOFILL_HINT_PASSWORD},
     * {@link #AUTOFILL_HINT_CREDIT_CARD_NUMBER},
     * {@link #AUTOFILL_HINT_CREDIT_CARD_SECURITY_CODE},
     * {@link #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DATE},
     * {@link #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_DAY},
     * {@link #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_MONTH},
     * {@link #AUTOFILL_HINT_CREDIT_CARD_EXPIRATION_YEAR}
     * are considered sensitive hints by the framework, and the list may include more hints
     * in the future.
     *
     * <p> The window hosting a sensitive view will be marked as secure during an active media
     * projection session. This would be equivalent to applying
     * {@link android.view.WindowManager.LayoutParams#FLAG_SECURE} to the window.
     *
     * @see #getContentSensitivity()
     */
    @FlaggedApi(FLAG_SENSITIVE_CONTENT_APP_PROTECTION_API)
    public static final int CONTENT_SENSITIVITY_AUTO = 0x0;

    /**
     * The view displays sensitive content.
     *
     * <p> The window hosting a sensitive view will be marked as secure during an active media
     * projection session. This would be equivalent to applying
     * {@link android.view.WindowManager.LayoutParams#FLAG_SECURE} to the window.
     *
     * @see #getContentSensitivity()
     */
    @FlaggedApi(FLAG_SENSITIVE_CONTENT_APP_PROTECTION_API)
    public static final int CONTENT_SENSITIVITY_SENSITIVE = 0x1;

    /**
     * The view doesn't display sensitive content.
     *
     * @see #getContentSensitivity()
     */
    @FlaggedApi(FLAG_SENSITIVE_CONTENT_APP_PROTECTION_API)
    public static final int CONTENT_SENSITIVITY_NOT_SENSITIVE = 0x2;

    /** @hide */
    @IntDef(flag = true, prefix = { "FOCUSABLES_" }, value = {
            FOCUSABLES_ALL,
            FOCUSABLES_TOUCH_MODE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusableMode {}

    /**
     * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
     * should add all focusable Views regardless if they are focusable in touch mode.
     */
    public static final int FOCUSABLES_ALL = 0x00000000;

    /**
     * View flag indicating whether {@link #addFocusables(ArrayList, int, int)}
     * should add only Views focusable in touch mode.
     */
    public static final int FOCUSABLES_TOUCH_MODE = 0x00000001;

    /** @hide */
    @IntDef(prefix = { "FOCUS_" }, value = {
            FOCUS_BACKWARD,
            FOCUS_FORWARD,
            FOCUS_LEFT,
            FOCUS_UP,
            FOCUS_RIGHT,
            FOCUS_DOWN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusDirection {}

    /** @hide */
    @IntDef(prefix = { "FOCUS_" }, value = {
            FOCUS_LEFT,
            FOCUS_UP,
            FOCUS_RIGHT,
            FOCUS_DOWN
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface FocusRealDirection {} // Like @FocusDirection, but without forward/backward

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the previous selectable
     * item.
     */
    public static final int FOCUS_BACKWARD = 0x00000001;

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the next selectable
     * item.
     */
    public static final int FOCUS_FORWARD = 0x00000002;

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the left.
     */
    public static final int FOCUS_LEFT = 0x00000011;

    /**
     * Use with {@link #focusSearch(int)}. Move focus up.
     */
    public static final int FOCUS_UP = 0x00000021;

    /**
     * Use with {@link #focusSearch(int)}. Move focus to the right.
     */
    public static final int FOCUS_RIGHT = 0x00000042;

    /**
     * Use with {@link #focusSearch(int)}. Move focus down.
     */
    public static final int FOCUS_DOWN = 0x00000082;

    /**
     * Bits of {@link #getMeasuredWidthAndState()} and
     * {@link #getMeasuredWidthAndState()} that provide the actual measured size.
     */
    public static final int MEASURED_SIZE_MASK = 0x00ffffff;

    /**
     * Bits of {@link #getMeasuredWidthAndState()} and
     * {@link #getMeasuredWidthAndState()} that provide the additional state bits.
     */
    public static final int MEASURED_STATE_MASK = 0xff000000;

    /**
     * Bit shift of {@link #MEASURED_STATE_MASK} to get to the height bits
     * for functions that combine both width and height into a single int,
     * such as {@link #getMeasuredState()} and the childState argument of
     * {@link #resolveSizeAndState(int, int, int)}.
     */
    public static final int MEASURED_HEIGHT_STATE_SHIFT = 16;

    /**
     * Bit of {@link #getMeasuredWidthAndState()} and
     * {@link #getMeasuredWidthAndState()} that indicates the measured size
     * is smaller that the space the view would like to have.
     */
    public static final int MEASURED_STATE_TOO_SMALL = 0x01000000;

    /**
     * Base View state sets
     */
    // Singles
    /**
     * Indicates the view has no states set. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] EMPTY_STATE_SET;
    /**
     * Indicates the view is enabled. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] ENABLED_STATE_SET;
    /**
     * Indicates the view is focused. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] FOCUSED_STATE_SET;
    /**
     * Indicates the view is selected. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] PRESSED_STATE_SET;
    /**
     * Indicates the view's window has focus. States are used with
     * {@link android.graphics.drawable.Drawable} to change the drawing of the
     * view depending on its state.
     *
     * @see android.graphics.drawable.Drawable
     * @see #getDrawableState()
     */
    protected static final int[] WINDOW_FOCUSED_STATE_SET;
    // Doubles
    /**
     * Indicates the view is enabled and has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_STATE_SET;
    /**
     * Indicates the view is enabled and selected.
     *
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] ENABLED_SELECTED_STATE_SET;
    /**
     * Indicates the view is enabled and that its window has focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is focused and selected.
     *
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view has the focus and that its window has the focus.
     *
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is selected and that its window has the focus.
     *
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] SELECTED_WINDOW_FOCUSED_STATE_SET;
    // Triples
    /**
     * Indicates the view is enabled, focused and selected.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is enabled, focused and its window has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is enabled, selected and its window has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is focused, selected and its window has the focus.
     *
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is enabled, focused, selected and its window
     * has the focus.
     *
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] PRESSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, selected and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and focused.
     *
     * @see #PRESSED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, focused and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, focused and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, focused, selected and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed and enabled.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled and its window has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, selected and its window has the
     * focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled and focused.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, focused and its window has the
     * focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, focused and selected.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET;
    /**
     * Indicates the view is pressed, enabled, focused, selected and its window
     * has the focus.
     *
     * @see #PRESSED_STATE_SET
     * @see #ENABLED_STATE_SET
     * @see #SELECTED_STATE_SET
     * @see #FOCUSED_STATE_SET
     * @see #WINDOW_FOCUSED_STATE_SET
     */
    protected static final int[] PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET;

    /**
     * This indicates that the frame rate category was chosen for an unknown reason.
     * @hide
     */
    public static final int FRAME_RATE_CATEGORY_REASON_UNKNOWN = 0x0000_0000;

    /**
     * This indicates that the frame rate category was chosen because it was a small area update.
     * @hide
     */
    public static final int FRAME_RATE_CATEGORY_REASON_SMALL = 0x0100_0000;

    /**
     * This indicates that the frame rate category was chosen because it was an intermittent update.
     * @hide
     */
    public static final int FRAME_RATE_CATEGORY_REASON_INTERMITTENT = 0x0200_0000;

    /**
     * This indicates that the frame rate category was chosen because it was a large View.
     * @hide
     */
    public static final int FRAME_RATE_CATEGORY_REASON_LARGE = 0x03000000;

    /**
     * This indicates that the frame rate category was chosen because it was requested.
     * @hide
     */
    public static final int FRAME_RATE_CATEGORY_REASON_REQUESTED = 0x0400_0000;

    /**
     * This indicates that the frame rate category was chosen because an invalid frame rate was
     * requested.
     * @hide
     */
    public static final int FRAME_RATE_CATEGORY_REASON_INVALID = 0x0500_0000;

    /**
     * This indicates that the frame rate category was chosen because the view has a velocity
     * @hide
     */
    public static final int FRAME_RATE_CATEGORY_REASON_VELOCITY = 0x0600_0000;

    /**
     * This indicates that the frame rate category was chosen because it is currently boosting.
     * @hide
     */
    public static final int FRAME_RATE_CATEGORY_REASON_BOOST = 0x0800_0000;

    /**
     * This indicates that the frame rate category was chosen because it is currently having
     * touch boost.
     * @hide
     */
    public static final int FRAME_RATE_CATEGORY_REASON_TOUCH = 0x0900_0000;

    /**
     * This indicates that the frame rate category was chosen because it is currently having
     * touch boost.
     * @hide
     */
    public static final int FRAME_RATE_CATEGORY_REASON_CONFLICTED = 0x0A00_0000;

    private static final int FRAME_RATE_CATEGORY_REASON_MASK = 0xFFFF_0000;

    /**
     * @hide
     */
    protected static boolean sToolkitSetFrameRateReadOnlyFlagValue;
    private static boolean sToolkitMetricsForFrameRateDecisionFlagValue;
    private static final boolean sToolkitFrameRateDefaultNormalReadOnlyFlagValue =
            toolkitFrameRateDefaultNormalReadOnly();
    private static final boolean sToolkitFrameRateBySizeReadOnlyFlagValue =
            toolkitFrameRateBySizeReadOnly();
    private static final boolean sToolkitFrameRateSmallUsesPercentReadOnlyFlagValue =
            toolkitFrameRateSmallUsesPercentReadOnly();
    private static final boolean sToolkitFrameRateViewEnablingReadOnlyFlagValue =
            toolkitFrameRateViewEnablingReadOnly();
    private static boolean sToolkitFrameRateVelocityMappingReadOnlyFlagValue =
            toolkitFrameRateVelocityMappingReadOnly();
    private static boolean sToolkitFrameRateAnimationBugfix25q1FlagValue =
            toolkitFrameRateAnimationBugfix25q1();
    private static boolean sToolkitViewGroupFrameRateApiFlagValue =
            toolkitViewgroupSetRequestedFrameRateApi();

    // Used to set frame rate compatibility.
    @Surface.FrameRateCompatibility int mFrameRateCompatibility =
            FRAME_RATE_COMPATIBILITY_FIXED_SOURCE;

    static {
        EMPTY_STATE_SET = StateSet.get(0);

        WINDOW_FOCUSED_STATE_SET = StateSet.get(StateSet.VIEW_STATE_WINDOW_FOCUSED);

        SELECTED_STATE_SET = StateSet.get(StateSet.VIEW_STATE_SELECTED);
        SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED);

        FOCUSED_STATE_SET = StateSet.get(StateSet.VIEW_STATE_FOCUSED);
        FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_FOCUSED);
        FOCUSED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_FOCUSED);
        FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_FOCUSED);

        ENABLED_STATE_SET = StateSet.get(StateSet.VIEW_STATE_ENABLED);
        ENABLED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_ENABLED);
        ENABLED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_ENABLED);
        ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_ENABLED);
        ENABLED_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_FOCUSED | StateSet.VIEW_STATE_ENABLED);
        ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_ENABLED);
        ENABLED_FOCUSED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_ENABLED);
        ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_FOCUSED| StateSet.VIEW_STATE_ENABLED);

        PRESSED_STATE_SET = StateSet.get(StateSet.VIEW_STATE_PRESSED);
        PRESSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_FOCUSED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_FOCUSED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_FOCUSED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_ENABLED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_ENABLED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_ENABLED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_ENABLED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_FOCUSED | StateSet.VIEW_STATE_ENABLED
                        | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_FOCUSED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_ENABLED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_FOCUSED_SELECTED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_SELECTED | StateSet.VIEW_STATE_FOCUSED
                        | StateSet.VIEW_STATE_ENABLED | StateSet.VIEW_STATE_PRESSED);
        PRESSED_ENABLED_FOCUSED_SELECTED_WINDOW_FOCUSED_STATE_SET = StateSet.get(
                StateSet.VIEW_STATE_WINDOW_FOCUSED | StateSet.VIEW_STATE_SELECTED
                        | StateSet.VIEW_STATE_FOCUSED| StateSet.VIEW_STATE_ENABLED
                        | StateSet.VIEW_STATE_PRESSED);

        sToolkitSetFrameRateReadOnlyFlagValue = toolkitSetFrameRateReadOnly();
        sToolkitMetricsForFrameRateDecisionFlagValue = toolkitMetricsForFrameRateDecision();
        sCalculateBoundsInParentFromBoundsInScreenFlagValue =
                calculateBoundsInParentFromBoundsInScreen();
        sUseMeasureCacheDuringForceLayoutFlagValue = enableUseMeasureCacheDuringForceLayout();
    }

    /**
     * Accessibility event types that are dispatched for text population.
     */
private static final int POPULATING_ACCESSIBILITY_EVENT_TYPES =
            AccessibilityEvent.TYPE_VIEW_CLICKED
            | AccessibilityEvent.TYPE_VIEW_LONG_CLICKED
            | AccessibilityEvent.TYPE_VIEW_SELECTED
            | AccessibilityEvent.TYPE_VIEW_FOCUSED
            | AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            | AccessibilityEvent.TYPE_VIEW_HOVER_ENTER
            | AccessibilityEvent.TYPE_VIEW_HOVER_EXIT
            | AccessibilityEvent.TYPE_VIEW_TEXT_CHANGED
            | AccessibilityEvent.TYPE_VIEW_TEXT_SELECTION_CHANGED
            | AccessibilityEvent.TYPE_VIEW_ACCESSIBILITY_FOCUSED
            | AccessibilityEvent.TYPE_VIEW_TEXT_TRAVERSED_AT_MOVEMENT_GRANULARITY;

    static final int DEBUG_CORNERS_COLOR = Color.rgb(63, 127, 255);

    static final int DEBUG_CORNERS_SIZE_DIP = 8;

    /**
     * Temporary Rect currently for use in setBackground().  This will probably
     * be extended in the future to hold our own class with more than just
     * a Rect. :)
     */
    static final ThreadLocal<Rect> sThreadLocal = ThreadLocal.withInitial(Rect::new);

    /**
     * Map used to store views' tags.
     */
    @UnsupportedAppUsage
    private SparseArray<Object> mKeyedTags;

    /**
     * The next available accessibility id.
     */
    private static int sNextAccessibilityViewId;

    /**
     * The animation currently associated with this view.
     * @hide
     */
    protected Animation mCurrentAnimation = null;

    /**
     * Width as measured during measure pass.
     * {@hide}
     */
    @ViewDebug.ExportedProperty(category = "measurement")
    @UnsupportedAppUsage
    int mMeasuredWidth;

    /**
     * Height as measured during measure pass.
     * {@hide}
     */
    @ViewDebug.ExportedProperty(category = "measurement")
    @UnsupportedAppUsage
    int mMeasuredHeight;

    /**
     * Flag to indicate that this view was marked INVALIDATED, or had its display list
     * invalidated, prior to the current drawing iteration. If true, the view must re-draw
     * its display list. This flag, used only when hw accelerated, allows us to clear the
     * flag while retaining this information until it's needed (at getDisplayList() time and
     * in drawChild(), when we decide to draw a view's children's display lists into our own).
     *
     * {@hide}
     */
    @UnsupportedAppUsage
    boolean mRecreateDisplayList = false;

    /**
     * The view's identifier.
     * {@hide}
     *
     * @see #setId(int)
     * @see #getId()
     */
    @IdRes
    @ViewDebug.ExportedProperty(resolveId = true)
    int mID = NO_ID
 /** The ID of this view for autofill purposes.
     * <ul>
     *     <li>== {@link #NO_ID}: ID has not been assigned yet
     *     <li>&le; {@link #LAST_APP_AUTOFILL_ID}: View is not part of a activity. The ID is
     *                                                  unique in the process. This might change
     *                                                  over activity lifecycle events.
     *     <li>&gt; {@link #LAST_APP_AUTOFILL_ID}: View is part of a activity. The ID is
     *                                                  unique in the activity. This stays the same
     *                                                  over activity lifecycle events.
     */
    private int mAutofillViewId = NO_ID;

    // ID for accessibility purposes. This ID must be unique for every window
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    private int mAccessibilityViewId = NO_ID;

    private int mAccessibilityCursorPosition = ACCESSIBILITY_CURSOR_POSITION_UNDEFINED;

    /**
     * The view's tag.
     * {@hide}
     *
     * @see #setTag(Object)
     * @see #getTag()
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    protected Object mTag = null;
 /*
     * Masks for mPrivateFlags, as generated by dumpFlags():
     *
     * |-------|-------|-------|-------|
     *                                 1 PFLAG_WANTS_FOCUS
     *                                1  PFLAG_FOCUSED
     *                               1   PFLAG_SELECTED
     *                              1    PFLAG_IS_ROOT_NAMESPACE
     *                             1     PFLAG_HAS_BOUNDS
     *                            1      PFLAG_DRAWN
     *                           1       PFLAG_DRAW_ANIMATION
     *                          1        PFLAG_SKIP_DRAW
     *                        1          PFLAG_REQUEST_TRANSPARENT_REGIONS
     *                       1           PFLAG_DRAWABLE_STATE_DIRTY
     *                      1            PFLAG_MEASURED_DIMENSION_SET
     *                     1             PFLAG_FORCE_LAYOUT
     *                    1              PFLAG_LAYOUT_REQUIRED
     *                   1               PFLAG_PRESSED
     *                  1                PFLAG_DRAWING_CACHE_VALID
     *                 1                 PFLAG_ANIMATION_STARTED
     *                1                  PFLAG_SAVE_STATE_CALLED
     *               1                   PFLAG_ALPHA_SET
     *              1                    PFLAG_SCROLL_CONTAINER
     *             1                     PFLAG_SCROLL_CONTAINER_ADDED
     *            1                      PFLAG_DIRTY
     *            1                      PFLAG_DIRTY_MASK
     *          1                        PFLAG_OPAQUE_BACKGROUND
     *         1                         PFLAG_OPAQUE_SCROLLBARS
     *         11                        PFLAG_OPAQUE_MASK
     *        1                          PFLAG_PREPRESSED
     *       1                           PFLAG_CANCEL_NEXT_UP_EVENT
     *      1                            PFLAG_AWAKEN_SCROLL_BARS_ON_ATTACH
     *     1                             PFLAG_HOVERED
     *    1                              PFLAG_NOTIFY_AUTOFILL_MANAGER_ON_CLICK
     *   1                               PFLAG_ACTIVATED
     *  1                                PFLAG_INVALIDATED
     * |-------|-------|-------|-------|
     */
    /** {@hide} */
    static final int PFLAG_WANTS_FOCUS                 = 0x00000001;
    /** {@hide} */
    static final int PFLAG_FOCUSED                     = 0x00000002;
    /** {@hide} */
    static final int PFLAG_SELECTED                    = 0x00000004;
    /** {@hide} */
    static final int PFLAG_IS_ROOT_NAMESPACE           = 0x00000008;
    /** {@hide} */
    static final int PFLAG_HAS_BOUNDS                  = 0x00000010;
    /** {@hide} */
    static final int PFLAG_DRAWN                       = 0x00000020;
    /**
     * When this flag is set, this view is running an animation on behalf of its
     * children and should therefore not cancel invalidate requests, even if they
     * lie outside of this view's bounds.
     *
     * {@hide}
     */
    static final int PFLAG_DRAW_ANIMATION              = 0x00000040;
    /** {@hide} */
    static final int PFLAG_SKIP_DRAW                   = 0x00000080;
    /** {@hide} */
    static final int PFLAG_REQUEST_TRANSPARENT_REGIONS = 0x00000200;
    /** {@hide} */
    static final int PFLAG_DRAWABLE_STATE_DIRTY        = 0x00000400;
    /** {@hide} */
    static final int PFLAG_MEASURED_DIMENSION_SET      = 0x00000800;
    /** {@hide} */
    static final int PFLAG_FORCE_LAYOUT                = 0x00001000;
    /** {@hide} */
    static final int PFLAG_LAYOUT_REQUIRED             = 0x00002000;

    private static final int PFLAG_PRESSED             = 0x00004000;

    /** {@hide} */
    static final int PFLAG_DRAWING_CACHE_VALID         = 0x00008000;
    /**
     * Flag used to indicate that this view should be drawn once more (and only once
     * more) after its animation has completed.
     * {@hide}
     */
    static final int PFLAG_ANIMATION_STARTED           = 0x00010000;

    private static final int PFLAG_SAVE_STATE_CALLED   = 0x00020000;

    /**
     * Indicates that the View returned true when onSetAlpha() was called and that
     * the alpha must be restored.
     * {@hide}
     */
    static final int PFLAG_ALPHA_SET                   = 0x00040000;

    /**
     * Set by {@link #setScrollContainer(boolean)}.
     */
    static final int PFLAG_SCROLL_CONTAINER            = 0x00080000;

    /**
     * Set by {@link #setScrollContainer(boolean)}.
     */
    static final int PFLAG_SCROLL_CONTAINER_ADDED      = 0x00100000;

    /**
     * View flag indicating whether this view was invalidated (fully or partially.)
     *
     * @hide
     */
    static final int PFLAG_DIRTY                       = 0x00200000;

    /**
     * Mask for {@link #PFLAG_DIRTY}.
     *
     * @hide
     */
    static final int PFLAG_DIRTY_MASK                  = 0x00200000;

    /**
     * Indicates whether the background is opaque.
     *
     * @hide
     */
    static final int PFLAG_OPAQUE_BACKGROUND           = 0x00800000;

    /**
     * Indicates whether the scrollbars are opaque.
     *
     * @hide
     */
    static final int PFLAG_OPAQUE_SCROLLBARS           = 0x01000000;

    /**
     * Indicates whether the view is opaque.
     *
     * @hide
     */
    static final int PFLAG_OPAQUE_MASK                 = 0x01800000;

    /**
     * Indicates a prepressed state;
     * the short time between ACTION_DOWN and recognizing
     * a 'real' press. Prepressed is used to recognize quick taps
     * even when they are shorter than ViewConfiguration.getTapTimeout().
     *
     * @hide
     */
    private static final int PFLAG_PREPRESSED          = 0x02000000;

    /**
     * Indicates whether the view is temporarily detached.
     *
     * @hide
     */
    static final int PFLAG_CANCEL_NEXT_UP_EVENT        = 0x04000000;

    /**
     * Indicates that we should awaken scroll bars once attached
     *
     * PLEASE NOTE: This flag is now unused as we now send onVisibilityChanged
     * during window attachment and it is no longer needed. Feel free to repurpose it.
     *
     * @hide
     */
    private static final int PFLAG_AWAKEN_SCROLL_BARS_ON_ATTACH = 0x08000000;

    /**
     * Indicates that the view has received HOVER_ENTER.  Cleared on HOVER_EXIT.
     * @hide
     */
    private static final int PFLAG_HOVERED             = 0x10000000;

    /**
     * Flag set by {@link AutofillManager} if it needs to be notified when this view is clicked.
     */
    private static final int PFLAG_NOTIFY_AUTOFILL_MANAGER_ON_CLICK = 0x20000000;

    /** {@hide} */
    static final int PFLAG_ACTIVATED                   = 0x40000000;

    /**
     * Indicates that this view was specifically invalidated, not just dirtied because some
     * child view was invalidated. The flag is used to determine when we need to recreate
     * a view's display list (as opposed to just returning a reference to its existing
     * display list).
     *
     * @hide
     */
    static final int PFLAG_INVALIDATED                 = 0x80000000;

    /* End of masks for mPrivateFlags */

    /*
     * Masks for mPrivateFlags2, as generated by dumpFlags():
     *
     * |-------|-------|-------|-------|
     *                                 1 PFLAG2_DRAG_CAN_ACCEPT
     *                                1  PFLAG2_DRAG_HOVERED
     *                              11   PFLAG2_LAYOUT_DIRECTION_MASK
     *                             1     PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL
     *                            1      PFLAG2_LAYOUT_DIRECTION_RESOLVED
     *                            11     PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK
     *                           1       PFLAG2_TEXT_DIRECTION_FLAGS[1]
     *                          1        PFLAG2_TEXT_DIRECTION_FLAGS[2]
     *                          11       PFLAG2_TEXT_DIRECTION_FLAGS[3]
     *                         1         PFLAG2_TEXT_DIRECTION_FLAGS[4]
     *                         1 1       PFLAG2_TEXT_DIRECTION_FLAGS[5]
     *                         11        PFLAG2_TEXT_DIRECTION_FLAGS[6]
     *                         111       PFLAG2_TEXT_DIRECTION_FLAGS[7]
     *                         111       PFLAG2_TEXT_DIRECTION_MASK
     *                        1          PFLAG2_TEXT_DIRECTION_RESOLVED
     *                       1           PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT
     *                     111           PFLAG2_TEXT_DIRECTION_RESOLVED_MASK
     *                    1              PFLAG2_TEXT_ALIGNMENT_FLAGS[1]
     *                   1               PFLAG2_TEXT_ALIGNMENT_FLAGS[2]
     *                   11              PFLAG2_TEXT_ALIGNMENT_FLAGS[3]
     *                  1                PFLAG2_TEXT_ALIGNMENT_FLAGS[4]
     *                  1 1              PFLAG2_TEXT_ALIGNMENT_FLAGS[5]
     *                  11               PFLAG2_TEXT_ALIGNMENT_FLAGS[6]
     *                  111              PFLAG2_TEXT_ALIGNMENT_MASK
     *                 1                 PFLAG2_TEXT_ALIGNMENT_RESOLVED
     *                1                  PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT
     *              111                  PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK
     *           111                     PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_MASK
     *         11                        PFLAG2_ACCESSIBILITY_LIVE_REGION_MASK
     *       1                           PFLAG2_ACCESSIBILITY_FOCUSED
     *      1                            PFLAG2_SUBTREE_ACCESSIBILITY_STATE_CHANGED
     *     1                             PFLAG2_VIEW_QUICK_REJECTED
     *    1                              PFLAG2_PADDING_RESOLVED
     *   1                               PFLAG2_DRAWABLE_RESOLVED
     *  1                                PFLAG2_HAS_TRANSIENT_STATE
     * |-------|-------|-------|-------|
     */

    /**
     * Indicates that this view has reported that it can accept the current drag's content.
     * Cleared when the drag operation concludes.
     * @hide
     */
    static final int PFLAG2_DRAG_CAN_ACCEPT            = 0x00000001;

    /**
     * Indicates that this view is currently directly under the drag location in a
     * drag-and-drop operation involving content that it can accept.  Cleared when
     * the drag exits the view, or when the drag operation concludes.
     * @hide
     */
    static final int PFLAG2_DRAG_HOVERED               = 0x00000002;

    /** @hide */
    @IntDef(prefix = { "LAYOUT_DIRECTION_" }, value = {
            LAYOUT_DIRECTION_LTR,
            LAYOUT_DIRECTION_RTL,
            LAYOUT_DIRECTION_INHERIT,
            LAYOUT_DIRECTION_LOCALE
    })
    @Retention(RetentionPolicy.SOURCE)
    // Not called LayoutDirection to avoid conflict with android.util.LayoutDirection
    public @interface LayoutDir {}

    /** @hide */
    @IntDef(prefix = { "LAYOUT_DIRECTION_" }, value = {
            LAYOUT_DIRECTION_LTR,
            LAYOUT_DIRECTION_RTL
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ResolvedLayoutDir {}

    /**
     * A flag to indicate that the layout direction of this view has not been defined yet.
     * @hide
     */
    public static final int LAYOUT_DIRECTION_UNDEFINED = LayoutDirection.UNDEFINED;

    /**
     * Horizontal layout direction of this view is from Left to Right.
     * Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_LTR = LayoutDirection.LTR;

    /**
     * Horizontal layout direction of this view is from Right to Left.
     * Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_RTL = LayoutDirection.RTL;

    /**
     * Horizontal layout direction of this view is inherited from its parent.
     * Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_INHERIT = LayoutDirection.INHERIT;

    /**
     * Horizontal layout direction of this view is from deduced from the default language
     * script for the locale. Use with {@link #setLayoutDirection}.
     */
    public static final int LAYOUT_DIRECTION_LOCALE = LayoutDirection.LOCALE;

    /**
     * Bit shift to get the horizontal layout direction. (bits after DRAG_HOVERED)
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT = 2;

    /**
     * Mask for use with private flags indicating bits used for horizontal layout direction.
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_MASK = 0x00000003 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /**
     * Indicates whether the view horizontal layout direction has been resolved and drawn to the
     * right-to-left direction.
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED_RTL = 4 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /**
     * Indicates whether the view horizontal layout direction has been resolved.
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED = 8 << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /**
     * Mask for use with private flags indicating bits used for resolved horizontal layout direction.
     * @hide
     */
    static final int PFLAG2_LAYOUT_DIRECTION_RESOLVED_MASK = 0x0000000C
            << PFLAG2_LAYOUT_DIRECTION_MASK_SHIFT;

    /*
     * Array of horizontal layout direction flags for mapping attribute "layoutDirection" to correct
     * flag value.
     * @hide
     */
    private static final int[] LAYOUT_DIRECTION_FLAGS = {
            LAYOUT_DIRECTION_LTR,
            LAYOUT_DIRECTION_RTL,
            LAYOUT_DIRECTION_INHERIT,
            LAYOUT_DIRECTION_LOCALE
    };

    /**
     * Default horizontal layout direction.
     */
    private static final int LAYOUT_DIRECTION_DEFAULT = LAYOUT_DIRECTION_INHERIT;

    /**
     * Default horizontal layout direction.
     * @hide
     */
    static final int LAYOUT_DIRECTION_RESOLVED_DEFAULT = LAYOUT_DIRECTION_LTR;

    /**
     * Text direction is inherited through {@link ViewGroup}
     */
    public static final int TEXT_DIRECTION_INHERIT = 0;

    /**
     * Text direction is using "first strong algorithm". The first strong directional character
     * determines the paragraph direction. If there is no strong directional character, the
     * paragraph direction is the view's resolved layout direction.
     */
    public static final int TEXT_DIRECTION_FIRST_STRONG = 1;

    /**
     * Text direction is using "any-RTL" algorithm. The paragraph direction is RTL if it contains
     * any strong RTL character, otherwise it is LTR if it contains any strong LTR characters.
     * If there are neither, the paragraph direction is the view's resolved layout direction.
     */
    public static final int TEXT_DIRECTION_ANY_RTL = 2;

    /**
     * Text direction is forced to LTR.
     */
    public static final int TEXT_DIRECTION_LTR = 3;

    /**
     * Text direction is forced to RTL.
     */
    public static final int TEXT_DIRECTION_RTL = 4;

    /**
     * Text direction is coming from the system Locale.
     */
    public static final int TEXT_DIRECTION_LOCALE = 5;

    /**
     * Text direction is using "first strong algorithm". The first strong directional character
     * determines the paragraph direction. If there is no strong directional character, the
     * paragraph direction is LTR.
     */
    public static final int TEXT_DIRECTION_FIRST_STRONG_LTR = 6;

    /**
     * Text direction is using "first strong algorithm". The first strong directional character
     * determines the paragraph direction. If there is no strong directional character, the
     * paragraph direction is RTL.
     */
    public static final int TEXT_DIRECTION_FIRST_STRONG_RTL = 7;

    /**
     * Default text direction is inherited
     */
    private static final int TEXT_DIRECTION_DEFAULT = TEXT_DIRECTION_INHERIT;

    /**
     * Default resolved text direction
     * @hide
     */
    static final int TEXT_DIRECTION_RESOLVED_DEFAULT = TEXT_DIRECTION_FIRST_STRONG;

    /**
     * Bit shift to get the horizontal layout direction. (bits after LAYOUT_DIRECTION_RESOLVED)
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_MASK_SHIFT = 6;

    /**
     * Mask for use with private flags indicating bits used for text direction.
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_MASK = 0x00000007
            << PFLAG2_TEXT_DIRECTION_MASK_SHIFT;

    /**
     * Array of text direction flags for mapping attribute "textDirection" to correct
     * flag value.
     * @hide
     */
    private static final int[] PFLAG2_TEXT_DIRECTION_FLAGS = {
            TEXT_DIRECTION_INHERIT << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_FIRST_STRONG << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_ANY_RTL << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_LTR << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_RTL << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_LOCALE << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_FIRST_STRONG_LTR << PFLAG2_TEXT_DIRECTION_MASK_SHIFT,
            TEXT_DIRECTION_FIRST_STRONG_RTL << PFLAG2_TEXT_DIRECTION_MASK_SHIFT
    };

    /**
     * Indicates whether the view text direction has been resolved.
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED = 0x00000008
            << PFLAG2_TEXT_DIRECTION_MASK_SHIFT;

    /**
     * Bit shift to get the horizontal layout direction. (bits after DRAG_HOVERED)
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT = 10;

    /**
     * Mask for use with private flags indicating bits used for resolved text direction.
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_MASK = 0x00000007
            << PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT;

    /**
     * Indicates whether the view text direction has been resolved to the "first strong" heuristic.
     * @hide
     */
    static final int PFLAG2_TEXT_DIRECTION_RESOLVED_DEFAULT =
            TEXT_DIRECTION_RESOLVED_DEFAULT << PFLAG2_TEXT_DIRECTION_RESOLVED_MASK_SHIFT;

    /** @hide */
    @IntDef(prefix = { "TEXT_ALIGNMENT_" }, value = {
            TEXT_ALIGNMENT_INHERIT,
            TEXT_ALIGNMENT_GRAVITY,
            TEXT_ALIGNMENT_CENTER,
            TEXT_ALIGNMENT_TEXT_START,
            TEXT_ALIGNMENT_TEXT_END,
            TEXT_ALIGNMENT_VIEW_START,
            TEXT_ALIGNMENT_VIEW_END
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface TextAlignment {}

    /**
     * Default text alignment. The text alignment of this View is inherited from its parent.
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_INHERIT = 0;

    /**
     * Default for the root view. The gravity determines the text alignment, ALIGN_NORMAL,
     * ALIGN_CENTER, or ALIGN_OPPOSITE, which are relative to each paragraph's text direction.
     *
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_GRAVITY = 1;

    /**
     * Align to the start of the paragraph, e.g. ALIGN_NORMAL.
     *
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_TEXT_START = 2;

    /**
     * Align to the end of the paragraph, e.g. ALIGN_OPPOSITE.
     *
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_TEXT_END = 3;

    /**
     * Center the paragraph, e.g. ALIGN_CENTER.
     *
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_CENTER = 4;

    /**
     * Align to the start of the view, which is ALIGN_LEFT if the view's resolved
     * layoutDirection is LTR, and ALIGN_RIGHT otherwise.
     *
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_VIEW_START = 5;

    /**
     * Align to the end of the view, which is ALIGN_RIGHT if the view's resolved
     * layoutDirection is LTR, and ALIGN_LEFT otherwise.
     *
     * Use with {@link #setTextAlignment(int)}
     */
    public static final int TEXT_ALIGNMENT_VIEW_END = 6;

    /**
     * Default text alignment is inherited
     */
    private static final int TEXT_ALIGNMENT_DEFAULT = TEXT_ALIGNMENT_GRAVITY;

    /**
     * Default resolved text alignment
     * @hide
     */
    static final int TEXT_ALIGNMENT_RESOLVED_DEFAULT = TEXT_ALIGNMENT_GRAVITY;

    /**
      * Bit shift to get the horizontal layout direction. (bits after DRAG_HOVERED)
      * @hide
      */
    static final int PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT = 13;

    /**
      * Mask for use with private flags indicating bits used for text alignment.
      * @hide
      */
    static final int PFLAG2_TEXT_ALIGNMENT_MASK = 0x00000007 << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT;

    /**
     * Array of text direction flags for mapping attribute "textAlignment" to correct
     * flag value.
     * @hide
     */
    private static final int[] PFLAG2_TEXT_ALIGNMENT_FLAGS = {
            TEXT_ALIGNMENT_INHERIT << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_GRAVITY << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_TEXT_START << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_TEXT_END << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_CENTER << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_VIEW_START << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT,
            TEXT_ALIGNMENT_VIEW_END << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT
    };

    /**
     * Indicates whether the view text alignment has been resolved.
     * @hide
     */
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED = 0x00000008 << PFLAG2_TEXT_ALIGNMENT_MASK_SHIFT;

    /**
     * Bit shift to get the resolved text alignment.
     * @hide
     */
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT = 17;

    /**
     * Mask for use with private flags indicating bits used for text alignment.
     * @hide
     */
    static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK = 0x00000007
            << PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT;

    /**
     * Indicates whether if the view text alignment has been resolved to gravity
     */
    private static final int PFLAG2_TEXT_ALIGNMENT_RESOLVED_DEFAULT =
            TEXT_ALIGNMENT_RESOLVED_DEFAULT << PFLAG2_TEXT_ALIGNMENT_RESOLVED_MASK_SHIFT;

    // Accessiblity constants for mPrivateFlags2

    /**
     * Shift for the bits in {@link #mPrivateFlags2} related to the
     * "importantForAccessibility" attribute.
     */
    static final int PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_SHIFT = 20;

    /**
     * Automatically determine whether a view is important for accessibility.
     */
    public static final int IMPORTANT_FOR_ACCESSIBILITY_AUTO = 0x00000000;

    /**
     * The view is important for accessibility.
     */
    public static final int IMPORTANT_FOR_ACCESSIBILITY_YES = 0x00000001;

    /**
     * The view is not important for accessibility.
     */
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO = 0x00000002;

    /**
     * The view is not important for accessibility, nor are any of its
     * descendant views.
     */
    public static final int IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS = 0x00000004;

    /**
     * The default whether the view is important for accessibility.
     */
    static final int IMPORTANT_FOR_ACCESSIBILITY_DEFAULT = IMPORTANT_FOR_ACCESSIBILITY_AUTO;

    /**
     * Automatically determine whether the view should only allow interactions from
     * {@link android.accessibilityservice.AccessibilityService}s with the
     * {@link android.accessibilityservice.AccessibilityServiceInfo#isAccessibilityTool} property
     * set to true.
     *
     * <p>
     * Accessibility interactions from services without {@code isAccessibilityTool} set to true are
     * disallowed for any of the following conditions:
     * <li>this view sets {@link #getFilterTouchesWhenObscured()}.</li>
     * <li>any parent of this view returns true from {@link #isAccessibilityDataSensitive()}.</li>
     * </p>
     */
    public static final int ACCESSIBILITY_DATA_SENSITIVE_AUTO = 0x00000000;

    /**
     * Only allow interactions from {@link android.accessibilityservice.AccessibilityService}s
     * with the {@link android.accessibilityservice.AccessibilityServiceInfo#isAccessibilityTool}
     * property set to true.
     */
    public static final int ACCESSIBILITY_DATA_SENSITIVE_YES = 0x00000001;

    /**
     * Allow interactions from all {@link android.accessibilityservice.AccessibilityService}s,
     * regardless of their
     * {@link android.accessibilityservice.AccessibilityServiceInfo#isAccessibilityTool} property.
     */
    public static final int ACCESSIBILITY_DATA_SENSITIVE_NO = 0x00000002;

    /** @hide */
    @IntDef(prefix = { "ACCESSIBILITY_DATA_SENSITIVE_" }, value = {
            ACCESSIBILITY_DATA_SENSITIVE_AUTO,
            ACCESSIBILITY_DATA_SENSITIVE_YES,
            ACCESSIBILITY_DATA_SENSITIVE_NO,
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface AccessibilityDataSensitive {}

    /**
     * Mask for obtaining the bits which specify how to determine
     * whether a view is important for accessibility.
     */
    static final int PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_MASK = (IMPORTANT_FOR_ACCESSIBILITY_AUTO
        | IMPORTANT_FOR_ACCESSIBILITY_YES | IMPORTANT_FOR_ACCESSIBILITY_NO
        | IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS)
        << PFLAG2_IMPORTANT_FOR_ACCESSIBILITY_SHIFT;

    /**
     * Shift for the bits in {@link #mPrivateFlags2} related to the
     * "accessibilityLiveRegion" attribute.
     */
    static final int PFLAG2_ACCESSIBILITY_LIVE_REGION_SHIFT = 23;

    /**
     * Live region mode specifying that accessibility services should not
     * automatically announce changes to this view. This is the default live
     * region mode for most views.
     * <p>
     * Use with {@link #setAccessibilityLiveRegion(int)}.
     */
    public static final int ACCESSIBILITY_LIVE_REGION_NONE = 0x00000000;

    /**
     * Live region mode specifying that accessibility services should notify users of changes to
     * this view.
     * <p>
     * Use with {@link #setAccessibilityLiveRegion(int)}.
     */
    public static final int ACCESSIBILITY_LIVE_REGION_POLITE = 0x00000001;

    /**
     * Live region mode specifying that accessibility services should immediately notify users of
     * changes to this view. For example, a screen reader may interrupt ongoing speech to
     * immediately announce these changes.
     * <p>
     * Use with {@link #setAccessibilityLiveRegion(int)}.
     */
    public static final int ACCESSIBILITY_LIVE_REGION_ASSERTIVE = 0x00000002;

    /**
     * The default whether the view is important for accessibility.
     */
    static final int ACCESSIBILITY_LIVE_REGION_DEFAULT = ACCESSIBILITY_LIVE_REGION_NONE;

    /**
     * Mask for obtaining the bits which specify a view's accessibility live
     * region mode.
     */
    static final int PFLAG2_ACCESSIBILITY_LIVE_REGION_MASK = (ACCESSIBILITY_LIVE_REGION_NONE
            | ACCESSIBILITY_LIVE_REGION_POLITE | ACCESSIBILITY_LIVE_REGION_ASSERTIVE)
            << PFLAG2_ACCESSIBILITY_LIVE_REGION_SHIFT;

    /**
     * Flag indicating whether a view has accessibility focus.
     */
    static final int PFLAG2_ACCESSIBILITY_FOCUSED = 0x04000000;

    /**
     * Flag whether the accessibility state of the subtree rooted at this view changed.
     */
    static final int PFLAG2_SUBTREE_ACCESSIBILITY_STATE_CHANGED = 0x08000000;

    /**
     * Flag indicating whether a view failed the quickReject() check in draw(). This condition
     * is used to check whether later changes to the view's transform should invalidate the
     * view to force the quickReject test to run again.
     */
    static final int PFLAG2_VIEW_QUICK_REJECTED = 0x10000000;

    /**
     * Flag indicating that start/end padding has been resolved into left/right padding
     * for use in measurement, layout, drawing, etc. This is set by {@link #resolvePadding()}
     * and checked by {@link #measure(int, int)} to determine if padding needs to be resolved
     * during measurement. In some special cases this is required such as when an adapter-based
     * view measures prospective children without attaching them to a window.
     */
    static final int PFLAG2_PADDING_RESOLVED = 0x20000000;

    /**
     * Flag indicating that the start/end drawables has been resolved into left/right ones.
     */
    static final int PFLAG2_DRAWABLE_RESOLVED = 0x40000000;

    /**
     * Indicates that the view is tracking some sort of transient state
     * that the app should not need to be aware of, but that the framework
     * should take special care to preserve.
     */
    static final int PFLAG2_HAS_TRANSIENT_STATE = 0x80000000;

    /**
     * Group of bits indicating that RTL properties resolution is done.
     */
    static final int ALL_RTL_PROPERTIES_RESOLVED = PFLAG2_LAYOUT_DIRECTION_RESOLVED |
            PFLAG2_TEXT_DIRECTION_RESOLVED |
            PFLAG2_TEXT_ALIGNMENT_RESOLVED |
            PFLAG2_PADDING_RESOLVED |
            PFLAG2_DRAWABLE_RESOLVED;

    // There are a couple of flags left in mPrivateFlags2

    /* End of masks for mPrivateFlags2 */

    /*
     * Masks for mPrivateFlags3, as generated by dumpFlags():
     *
     * |-------|-------|-------|-------|
     *                                 1 PFLAG3_VIEW_IS_ANIMATING_TRANSFORM
     *                                1  PFLAG3_VIEW_IS_ANIMATING_ALPHA
     *                               1   PFLAG3_IS_LAID_OUT
     *                              1    PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT
     *                             1     PFLAG3_CALLED_SUPER
     *                            1      PFLAG3_APPLYING_INSETS
     *                           1       PFLAG3_FITTING_SYSTEM_WINDOWS
     *                          1        PFLAG3_NESTED_SCROLLING_ENABLED
     *                         1         PFLAG3_SCROLL_INDICATOR_TOP
     *                        1          PFLAG3_SCROLL_INDICATOR_BOTTOM
     *                       1           PFLAG3_SCROLL_INDICATOR_LEFT
     *                      1            PFLAG3_SCROLL_INDICATOR_RIGHT
     *                     1             PFLAG3_SCROLL_INDICATOR_START
     *                    1              PFLAG3_SCROLL_INDICATOR_END
     *                   1               PFLAG3_ASSIST_BLOCKED
     *                  1                PFLAG3_CLUSTER
     *                 1                 PFLAG3_IS_AUTOFILLED
     *                1                  PFLAG3_FINGER_DOWN
     *               1                   PFLAG3_FOCUSED_BY_DEFAULT
     *           1111                    PFLAG3_IMPORTANT_FOR_AUTOFILL
     *          1                        PFLAG3_OVERLAPPING_RENDERING_FORCED_VALUE
     *         1                         PFLAG3_HAS_OVERLAPPING_RENDERING_FORCED
     *        1                          PFLAG3_TEMPORARY_DETACH
     *       1                           PFLAG3_NO_REVEAL_ON_FOCUS
     *      1                            PFLAG3_NOTIFY_AUTOFILL_ENTER_ON_LAYOUT
     *     1                             PFLAG3_SCREEN_READER_FOCUSABLE
     *    1                              PFLAG3_AGGREGATED_VISIBLE
     *   1                               PFLAG3_AUTOFILLID_EXPLICITLY_SET
     *  1                                PFLAG3_ACCESSIBILITY_HEADING
     * |-------|-------|-------|-------|
     */

    /**
     * Flag indicating that view has a transform animation set on it. This is used to track whether
     * an animation is cleared between successive frames, in order to tell the associated
     * DisplayList to clear its animation matrix.
     */
    static final int PFLAG3_VIEW_IS_ANIMATING_TRANSFORM = 0x1;

    /**
     * Flag indicating that view has an alpha animation set on it. This is used to track whether an
     * animation is cleared between successive frames, in order to tell the associated
     * DisplayList to restore its alpha value.
     */
    static final int PFLAG3_VIEW_IS_ANIMATING_ALPHA = 0x2;

    /**
     * Flag indicating that the view has been through at least one layout since it
     * was last attached to a window.
     */
    static final int PFLAG3_IS_LAID_OUT = 0x4;

    /**
     * Flag indicating that a call to measure() was skipped and should be done
     * instead when layout() is invoked.
     */
    static final int PFLAG3_MEASURE_NEEDED_BEFORE_LAYOUT = 0x8;

    /**
     * Flag indicating that an overridden method correctly called down to
     * the superclass implementation as required by the API spec.
     */
    static final int PFLAG3_CALLED_SUPER = 0x10;

    /**
     * Flag indicating that we're in the process of applying window insets.
     */
    static final int PFLAG3_APPLYING_INSETS = 0x20;

    /**
     * Flag indicating that we're in the process of fitting system windows using the old method.
     */
    static final int PFLAG3_FITTING_SYSTEM_WINDOWS = 0x40;

    /**
     * Flag indicating that nested scrolling is enabled for this view.
     * The view will optionally cooperate with views up its parent chain to allow for
     * integrated nested scrolling along the same axis.
     */
    static final int PFLAG3_NESTED_SCROLLING_ENABLED = 0x80;

    /**
     * Flag indicating that the bottom scroll indicator should be displayed
     * when this view can scroll up.
     */
    static final int PFLAG3_SCROLL_INDICATOR_TOP = 0x0100;

    /**
     * Flag indicating that the bottom scroll indicator should be displayed
     * when this view can scroll down.
     */
    static final int PFLAG3_SCROLL_INDICATOR_BOTTOM = 0x0200;

    /**
     * Flag indicating that the left scroll indicator should be displayed
     * when this view can scroll left.
     */
    static final int PFLAG3_SCROLL_INDICATOR_LEFT = 0x0400;

    /**
     * Flag indicating that the right scroll indicator should be displayed
     * when this view can scroll right.
     */
    static final int PFLAG3_SCROLL_INDICATOR_RIGHT = 0x0800;

    /**
     * Flag indicating that the start scroll indicator should be displayed
     * when this view can scroll in the start direction.
     */
    static final int PFLAG3_SCROLL_INDICATOR_START = 0x1000;

    /**
     * Flag indicating that the end scroll indicator should be displayed
     * when this view can scroll in the end direction.
     */
    static final int PFLAG3_SCROLL_INDICATOR_END = 0x2000;

    static final int DRAG_MASK = PFLAG2_DRAG_CAN_ACCEPT | PFLAG2_DRAG_HOVERED;

    static final int SCROLL_INDICATORS_NONE = 0x0000;

    /**
     * Mask for use with setFlags indicating bits used for indicating which
     * scroll indicators are enabled.
     */
    static final int SCROLL_INDICATORS_PFLAG3_MASK = PFLAG3_SCROLL_INDICATOR_TOP
            | PFLAG3_SCROLL_INDICATOR_BOTTOM | PFLAG3_SCROLL_INDICATOR_LEFT
            | PFLAG3_SCROLL_INDICATOR_RIGHT | PFLAG3_SCROLL_INDICATOR_START
            | PFLAG3_SCROLL_INDICATOR_END;

    /**
     * Left-shift required to translate between public scroll indicator flags
     * and internal PFLAGS3 flags. When used as a right-shift, translates
     * PFLAGS3 flags to public flags.
     */
    static final int SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT = 8;

    /** @hide */
    @Retention(RetentionPolicy.SOURCE)
    @IntDef(flag = true, prefix = { "SCROLL_INDICATOR_" }, value = {
            SCROLL_INDICATOR_TOP,
            SCROLL_INDICATOR_BOTTOM,
            SCROLL_INDICATOR_LEFT,
            SCROLL_INDICATOR_RIGHT,
            SCROLL_INDICATOR_START,
            SCROLL_INDICATOR_END,
    })
    public @interface ScrollIndicators {}

    /**
     * Scroll indicator direction for the top edge of the view.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_TOP =
            PFLAG3_SCROLL_INDICATOR_TOP >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Scroll indicator direction for the bottom edge of the view.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_BOTTOM =
            PFLAG3_SCROLL_INDICATOR_BOTTOM >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Scroll indicator direction for the left edge of the view.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_LEFT =
            PFLAG3_SCROLL_INDICATOR_LEFT >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Scroll indicator direction for the right edge of the view.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_RIGHT =
            PFLAG3_SCROLL_INDICATOR_RIGHT >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Scroll indicator direction for the starting edge of the view.
     * <p>
     * Resolved according to the view's layout direction, see
     * {@link #getLayoutDirection()} for more information.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_START =
            PFLAG3_SCROLL_INDICATOR_START >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * Scroll indicator direction for the ending edge of the view.
     * <p>
     * Resolved according to the view's layout direction, see
     * {@link #getLayoutDirection()} for more information.
     *
     * @see #setScrollIndicators(int)
     * @see #setScrollIndicators(int, int)
     * @see #getScrollIndicators()
     */
    public static final int SCROLL_INDICATOR_END =
            PFLAG3_SCROLL_INDICATOR_END >> SCROLL_INDICATORS_TO_PFLAGS3_LSHIFT;

    /**
     * <p>Indicates that we are allowing {@link ViewStructure} to traverse
     * into this view.<p>
     */
    static final int PFLAG3_ASSIST_BLOCKED = 0x4000;

    /**
     * Flag indicating that the view is a root of a keyboard navigation cluster.
     *
     * @see #isKeyboardNavigationCluster()
     * @see #setKeyboardNavigationCluster(boolean)
     */
    private static final int PFLAG3_CLUSTER = 0x8000;

    /**
     * Flag indicating that the view is autofilled
     *
     * @see #isAutofilled()
     * @see #setAutofilled(boolean, boolean)
     */
    private static final int PFLAG3_IS_AUTOFILLED = 0x10000;

    /**
     * Indicates that the user is currently touching the screen.
     * Currently used for the tooltip positioning only.
     */
    private static final int PFLAG3_FINGER_DOWN = 0x20000;

    /**
     * Flag indicating that this view is the default-focus view.
     *
     * @see #isFocusedByDefault()
     * @see #setFocusedByDefault(boolean)
     */
    private static final int PFLAG3_FOCUSED_BY_DEFAULT = 0x40000;

    /**
     * Shift for the bits in {@link #mPrivateFlags3} related to the
     * "importantForAutofill" attribute.
     */
    static final int PFLAG3_IMPORTANT_FOR_AUTOFILL_SHIFT = 19;

    /**
     * Mask for obtaining the bits which specify how to determine
     * whether a view is important for autofill.
     */
    static final int PFLAG3_IMPORTANT_FOR_AUTOFILL_MASK = (IMPORTANT_FOR_AUTOFILL_AUTO
            | IMPORTANT_FOR_AUTOFILL_YES | IMPORTANT_FOR_AUTOFILL_NO
            | IMPORTANT_FOR_AUTOFILL_YES_EXCLUDE_DESCENDANTS
            | IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS)
            << PFLAG3_IMPORTANT_FOR_AUTOFILL_SHIFT;

    /**
     * Whether this view has rendered elements that overlap (see {@link
     * #hasOverlappingRendering()}, {@link #forceHasOverlappingRendering(boolean)}, and
     * {@link #getHasOverlappingRendering()} ). The value in this bit is only valid when
     * PFLAG3_HAS_OVERLAPPING_RENDERING_FORCED has been set. Otherwise, the value is
     * determined by whatever {@link #hasOverlappingRendering()} returns.
     */
    private static final int PFLAG3_OVERLAPPING_RENDERING_FORCED_VALUE = 0x800000;

    /**
     * Whether {@link #forceHasOverlappingRendering(boolean)} has been called. When true, value
     * in PFLAG3_OVERLAPPING_RENDERING_FORCED_VALUE is valid.
     */
    private static final int PFLAG3_HAS_OVERLAPPING_RENDERING_FORCED = 0x1000000;

    /**
     * Flag indicating that the view is temporarily detached from the parent view.
     *
     * @see #onStartTemporaryDetach()
     * @see #onFinishTemporaryDetach()
     */
    static final int PFLAG3_TEMPORARY_DETACH = 0x2000000;

    /**
     * Flag indicating that the view does not wish to be revealed within its parent
     * hierarchy when it gains focus. Expressed in the negative since the historical
     * default behavior is to reveal on focus; this flag suppresses that behavior.
     *
     * @see #setRevealOnFocusHint(boolean)
     * @see #getRevealOnFocusHint()
     */
    private static final int PFLAG3_NO_REVEAL_ON_FOCUS = 0x4000000;

    /**
     * Flag indicating that when layout is completed we should notify
     * that the view was entered for autofill purposes. To minimize
     * showing autofill for views not visible to the user we evaluate
     * user visibility which cannot be done until the view is laid out.
     */
    static final int PFLAG3_NOTIFY_AUTOFILL_ENTER_ON_LAYOUT = 0x8000000;

    /**
     * Works like focusable for screen readers, but without the side effects on input focus.
     * @see #setScreenReaderFocusable(boolean)
     */
    private static final int PFLAG3_SCREEN_READER_FOCUSABLE = 0x10000000;

    /**
     * The last aggregated visibility. Used to detect when it truly changes.
     */
    private static final int PFLAG3_AGGREGATED_VISIBLE = 0x20000000;

    /**
     * Used to indicate that {@link #mAutofillId} was explicitly set through
     * {@link #setAutofillId(AutofillId)}.
     */
    private static final int PFLAG3_AUTOFILLID_EXPLICITLY_SET = 0x40000000;

    /**
     * Indicates if the View is a heading for accessibility purposes
     */
    private static final int PFLAG3_ACCESSIBILITY_HEADING = 0x80000000;

    /* End of masks for mPrivateFlags3 */

    /*
     * Masks for mPrivateFlags4, as generated by dumpFlags():
     *
     * |-------|-------|-------|-------|
     *                             1111 PFLAG4_IMPORTANT_FOR_CONTENT_CAPTURE_MASK
     *                            1     PFLAG4_NOTIFIED_CONTENT_CAPTURE_APPEARED
     *                           1      PFLAG4_NOTIFIED_CONTENT_CAPTURE_DISAPPEARED
     *                          1       PFLAG4_CONTENT_CAPTURE_IMPORTANCE_IS_CACHED
     *                         1        PFLAG4_CONTENT_CAPTURE_IMPORTANCE_CACHED_VALUE
     *                         11       PFLAG4_CONTENT_CAPTURE_IMPORTANCE_MASK
     *                        1         PFLAG4_FRAMEWORK_OPTIONAL_FITS_SYSTEM_WINDOWS
     *                       1          PFLAG4_AUTOFILL_HIDE_HIGHLIGHT
     *                     11           PFLAG4_SCROLL_CAPTURE_HINT_MASK
     *                    1             PFLAG4_ALLOW_CLICK_WHEN_DISABLED
     *                   1              PFLAG4_DETACHED
     *                  1               PFLAG4_HAS_TRANSLATION_TRANSIENT_STATE
     *                 1                PFLAG4_DRAG_A11Y_STARTED
     *                1                 PFLAG4_AUTO_HANDWRITING_INITIATION_ENABLED
     *               1                  PFLAG4_IMPORTANT_FOR_CREDENTIAL_MANAGER
     *              1                   PFLAG4_TRAVERSAL_TRACING_ENABLED
     *             1                    PFLAG4_RELAYOUT_TRACING_ENABLED
     *            1                     PFLAG4_ROTARY_HAPTICS_DETERMINED
     *           1                      PFLAG4_ROTARY_HAPTICS_ENABLED
     *          1                       PFLAG4_ROTARY_HAPTICS_SCROLL_SINCE_LAST_ROTARY_INPUT
     *         1                        PFLAG4_ROTARY_HAPTICS_WAITING_FOR_SCROLL_EVENT
     *       11                         PFLAG4_CONTENT_SENSITIVITY_MASK
     *      1                           PFLAG4_IS_COUNTED_AS_SENSITIVE
     *     1                            PFLAG4_HAS_DRAWN
     *    1                             PFLAG4_HAS_MOVED
     *   1                              PFLAG4_HAS_VIEW_PROPERTY_INVALIDATION
     *  1                               PFLAG4_FORCED_OVERRIDE_FRAME_RATE
     * 1                                PFLAG4_SELF_REQUESTED_FRAME_RATE
     * |-------|-------|-------|-------|
     */

    /**
     * Mask for obtaining the bits which specify how to determine
     * whether a view is important for autofill.
     *
     * <p>NOTE: the important for content capture values were the first flags added and are set in
     * the rightmost position, so we don't need to shift them
     */
    private static final int PFLAG4_IMPORTANT_FOR_CONTENT_CAPTURE_MASK =
            IMPORTANT_FOR_CONTENT_CAPTURE_AUTO | IMPORTANT_FOR_CONTENT_CAPTURE_YES
            | IMPORTANT_FOR_CONTENT_CAPTURE_NO
            | IMPORTANT_FOR_CONTENT_CAPTURE_YES_EXCLUDE_DESCENDANTS
            | IMPORTANT_FOR_CONTENT_CAPTURE_NO_EXCLUDE_DESCENDANTS;

    /*
     * Variables used to control when the IntelligenceManager.notifyNodeAdded()/removed() methods
     * should be called.
     *
     * The idea is to call notifyAppeared() after the view is layout and visible, then call
     * notifyDisappeared() when it's gone (without known when it was removed from the parent).
     */
    private static final int PFLAG4_NOTIFIED_CONTENT_CAPTURE_APPEARED = 0x10;
    private static final int PFLAG4_NOTIFIED_CONTENT_CAPTURE_DISAPPEARED = 0x20;

    /*
     * Flags used to cache the value returned by isImportantForContentCapture while the view
     * hierarchy is being traversed.
     */
    private static final int PFLAG4_CONTENT_CAPTURE_IMPORTANCE_IS_CACHED = 0x40;
    private static final int PFLAG4_CONTENT_CAPTURE_IMPORTANCE_CACHED_VALUE = 0x80;

    private static final int PFLAG4_CONTENT_CAPTURE_IMPORTANCE_MASK =
            PFLAG4_CONTENT_CAPTURE_IMPORTANCE_IS_CACHED
            | PFLAG4_CONTENT_CAPTURE_IMPORTANCE_CACHED_VALUE;

    /**
     * @see #OPTIONAL_FITS_SYSTEM_WINDOWS
     */
    static final int PFLAG4_FRAMEWORK_OPTIONAL_FITS_SYSTEM_WINDOWS = 0x000000100;

    /**
     * Flag indicating the field should not have yellow highlight when autofilled.
     */
    private static final int PFLAG4_AUTOFILL_HIDE_HIGHLIGHT = 0x200;

    /**
     * Shift for the bits in {@link #mPrivateFlags4} related to scroll capture.
     */
    static final int PFLAG4_SCROLL_CAPTURE_HINT_SHIFT = 10;

    static final int PFLAG4_SCROLL_CAPTURE_HINT_MASK = (SCROLL_CAPTURE_HINT_INCLUDE
            | SCROLL_CAPTURE_HINT_EXCLUDE | SCROLL_CAPTURE_HINT_EXCLUDE_DESCENDANTS)
            << PFLAG4_SCROLL_CAPTURE_HINT_SHIFT;

    /**
     * Indicates if the view can receive click events when disabled.
     */
    private static final int PFLAG4_ALLOW_CLICK_WHEN_DISABLED = 0x000001000;

    /**
     * Indicates if the view is just detached.
     */
    private static final int PFLAG4_DETACHED = 0x000002000;

    /**
     * Indicates that the view has transient state because the system is translating it.
     */
    private static final int PFLAG4_HAS_TRANSLATION_TRANSIENT_STATE = 0x000004000;

    /**
     * Indicates that the view has started a drag with {@link AccessibilityAction#ACTION_DRAG_START}
     */
    private static final int PFLAG4_DRAG_A11Y_STARTED = 0x000008000;

    /**
     * Indicates that the view enables auto handwriting initiation.
     */
    private static final int PFLAG4_AUTO_HANDWRITING_ENABLED = 0x000010000;

    /**
     * Indicates that the view is important for Credential Manager.
     */
    private static final int PFLAG4_IMPORTANT_FOR_CREDENTIAL_MANAGER = 0x000020000;

    /**
     * When set, measure and layout passes of this view will be logged with {@link Trace}, so we
     * can better debug jank due to complex view hierarchies.
     */
    private static final int PFLAG4_TRAVERSAL_TRACING_ENABLED = 0x000040000;

    /**
     * When set, emits a {@link Trace} instant event and stacktrace every time a requestLayout of
     * this class happens.
     */
    private static final int PFLAG4_RELAYOUT_TRACING_ENABLED = 0x000080000;

    /** Indicates if rotary scroll haptics support for the view has been determined. */
    private static final int PFLAG4_ROTARY_HAPTICS_DETERMINED = 0x100000;

    /**
     * Indicates if rotary scroll haptics is enabled for this view.
     * The source of truth for this info is a ViewConfiguration API; this bit only caches the value.
     */
    private static final int PFLAG4_ROTARY_HAPTICS_ENABLED = 0x200000;

    /** Indicates if there has been a scroll event since the last rotary input. */
    private static final int PFLAG4_ROTARY_HAPTICS_SCROLL_SINCE_LAST_ROTARY_INPUT = 0x400000;

    /**
     * Indicates if there has been a rotary input that may generate a scroll event.
     * This flag is important so that a scroll event can be properly attributed to a rotary input.
     */
    private static final int PFLAG4_ROTARY_HAPTICS_WAITING_FOR_SCROLL_EVENT = 0x800000;

    private static final int PFLAG4_CONTENT_SENSITIVITY_SHIFT = 24;

    /**
     * Mask for obtaining the bits which specify how to determine whether a view
     * displays sensitive content or not.
     */
    private static final int PFLAG4_CONTENT_SENSITIVITY_MASK =
            (CONTENT_SENSITIVITY_AUTO | CONTENT_SENSITIVITY_SENSITIVE
                    | CONTENT_SENSITIVITY_NOT_SENSITIVE) << PFLAG4_CONTENT_SENSITIVITY_SHIFT;

    /**
     * Whether this view has been counted as a sensitive view or not.
     *
     * @see AttachInfo#mSensitiveViewsCount
     */
    private static final int PFLAG4_IS_COUNTED_AS_SENSITIVE = 0x4000000;

    /**
     * Whether this view has been drawn once with updateDisplayListIfDirty() or not.
     * Used by VRR to for quick detection of scrolling.
     */
    private static final int PFLAG4_HAS_DRAWN = 0x8000000;

    /**
     * Whether this view has been moved with either setTranslationX/Y or setLeft/Top.
     * Used by VRR to for quick detection of scrolling.
     */
    private static final int PFLAG4_HAS_MOVED = 0x10000000;

    /**
     * Whether the invalidateViewProperty is involked at current frame.
     */
    private static final int PFLAG4_HAS_VIEW_PROPERTY_INVALIDATION = 0x20000000;

    /**
     * When set, this indicates whether the frame rate of the children should be
     * forcibly overridden, even if it has been explicitly configured by a user request.
     */
    private static final int PFLAG4_FORCED_OVERRIDE_FRAME_RATE = 0x40000000;

    /**
     * When set, this indicates that the frame rate is configured based on a user request.
     */
    private static final int PFLAG4_SELF_REQUESTED_FRAME_RATE = 0x80000000;

    /* End of masks for mPrivateFlags4 */

    /** @hide */
    protected static final int VIEW_STRUCTURE_FOR_ASSIST = 0;
    /** @hide */
    protected  static final int VIEW_STRUCTURE_FOR_AUTOFILL = 1;
    /** @hide */
    protected  static final int VIEW_STRUCTURE_FOR_CONTENT_CAPTURE = 2;

    /** @hide */
    @IntDef(flag = true, prefix = { "VIEW_STRUCTURE_FOR" }, value = {
            VIEW_STRUCTURE_FOR_ASSIST,
            VIEW_STRUCTURE_FOR_AUTOFILL,
            VIEW_STRUCTURE_FOR_CONTENT_CAPTURE
    })
    @Retention(RetentionPolicy.SOURCE)
    public @interface ViewStructureType {}

    /**
     * Always allow a user to over-scroll this view, provided it is a
     * view that can scroll.
     *
     * @see #getOverScrollMode()
     * @see #setOverScrollMode(int)
     */
    public static final int OVER_SCROLL_ALWAYS = 0;

    /**
     * Allow a user to over-scroll this view only if the content is large
     * enough to meaningfully scroll, provided it is a view that can scroll.
     *
     * @see #getOverScrollMode()
     * @see #setOverScrollMode(int)
     */
    public static final int OVER_SCROLL_IF_CONTENT_SCROLLS = 1;

    /**
     * Never allow a user to over-scroll this view.
     *
     * @see #getOverScrollMode()
     * @see #setOverScrollMode(int)
     */
    public static final int OVER_SCROLL_NEVER = 2;

    /**
     * Special constant for {@link #setSystemUiVisibility(int)}: View has
     * requested the system UI (status bar) to be visible (the default).
     *
     * @see #setSystemUiVisibility(int)
     * @deprecated SystemUiVisibility flags are deprecated. Use {@link WindowInsetsController}
     * instead.
     */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_VISIBLE = 0;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View has requested the
     * system UI to enter an unobtrusive "low profile" mode.
     *
     * <p>This is for use in games, book readers, video players, or any other
     * "immersive" application where the usual system chrome is deemed too distracting.
     *
     * <p>In low profile mode, the status bar and/or navigation icons may dim.
     *
     * @see #setSystemUiVisibility(int)
     * @deprecated Low profile mode is deprecated. Hide the system bars instead if the application
     * needs to be in a unobtrusive mode. Use {@link WindowInsetsController#hide(int)} with
     * {@link Type#systemBars()}.
     */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LOW_PROFILE = 0x00000001;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View has requested that the
     * system navigation be temporarily hidden.
     *
     * <p>This is an even less obtrusive state than that called for by
     * {@link #SYSTEM_UI_FLAG_LOW_PROFILE}; on devices that draw essential navigation controls
     * (Home, Back, and the like) on screen, <code>SYSTEM_UI_FLAG_HIDE_NAVIGATION</code> will cause
     * those to disappear. This is useful (in conjunction with the
     * {@link android.view.WindowManager.LayoutParams#FLAG_FULLSCREEN FLAG_FULLSCREEN} and
     * {@link android.view.WindowManager.LayoutParams#FLAG_LAYOUT_IN_SCREEN FLAG_LAYOUT_IN_SCREEN}
     * window flags) for displaying content using every last pixel on the display.
     *
     * <p>There is a limitation: because navigation controls are so important, the least user
     * interaction will cause them to reappear immediately.  When this happens, both
     * this flag and {@link #SYSTEM_UI_FLAG_FULLSCREEN} will be cleared automatically,
     * so that both elements reappear at the same time.
     *
     * @see #setSystemUiVisibility(int)
     * @deprecated Use {@link WindowInsetsController#hide(int)} with {@link Type#navigationBars()}
     * instead.
     */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_HIDE_NAVIGATION = 0x00000002;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View has requested to go
     * into the normal fullscreen mode so that its content can take over the screen
     * while still allowing the user to interact with the application.
     *
     * <p>This has the same visual effect as
     * {@link android.view.WindowManager.LayoutParams#FLAG_FULLSCREEN
     * WindowManager.LayoutParams.FLAG_FULLSCREEN},
     * meaning that non-critical screen decorations (such as the status bar) will be
     * hidden while the user is in the View's window, focusing the experience on
     * that content.  Unlike the window flag, if you are using ActionBar in
     * overlay mode with {@link Window#FEATURE_ACTION_BAR_OVERLAY
     * Window.FEATURE_ACTION_BAR_OVERLAY}, then enabling this flag will also
     * hide the action bar.
     *
     * <p>This approach to going fullscreen is best used over the window flag when
     * it is a transient state -- that is, the application does this at certain
     * points in its user interaction where it wants to allow the user to focus
     * on content, but not as a continuous state.  For situations where the application
     * would like to simply stay full screen the entire time (such as a game that
     * wants to take over the screen), the
     * {@link android.view.WindowManager.LayoutParams#FLAG_FULLSCREEN window flag}
     * is usually a better approach.  The state set here will be removed by the system
     * in various situations (such as the user moving to another application) like
     * the other system UI states.
     *
     * <p>When using this flag, the application should provide some easy facility
     * for the user to go out of it.  A common example would be in an e-book
     * reader, where tapping on the screen brings back whatever screen and UI
     * decorations that had been hidden while the user was immersed in reading
     * the book.
     *
     * @see #setSystemUiVisibility(int)
     * @deprecated Use {@link WindowInsetsController#hide(int)} with {@link Type#statusBars()}
     * instead.
     */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_FULLSCREEN = 0x00000004;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: When using other layout
     * flags, we would like a stable view of the content insets given to
     * {@link #fitSystemWindows(Rect)}.  This means that the insets seen there
     * will always represent the worst case that the application can expect
     * as a continuous state.  In the stock Android UI this is the space for
     * the system bar, nav bar, and status bar, but not more transient elements
     * such as an input method.
     *
     * The stable layout your UI sees is based on the system UI modes you can
     * switch to.  That is, if you specify {@link #SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN}
     * then you will get a stable layout for changes of the
     * {@link #SYSTEM_UI_FLAG_FULLSCREEN} mode; if you specify
     * {@link #SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN} and
     * {@link #SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION}, then you can transition
     * to {@link #SYSTEM_UI_FLAG_FULLSCREEN} and {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}
     * with a stable layout.  (Note that you should avoid using
     * {@link #SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION} by itself.)
     *
     * If you have set the window flag {@link WindowManager.LayoutParams#FLAG_FULLSCREEN}
     * to hide the status bar (instead of using {@link #SYSTEM_UI_FLAG_FULLSCREEN}),
     * then a hidden status bar will be considered a "stable" state for purposes
     * here.  This allows your UI to continually hide the status bar, while still
     * using the system UI flags to hide the action bar while still retaining
     * a stable layout.  Note that changing the window fullscreen flag will never
     * provide a stable layout for a clean transition.
     *
     * <p>If you are using ActionBar in
     * overlay mode with {@link Window#FEATURE_ACTION_BAR_OVERLAY
     * Window.FEATURE_ACTION_BAR_OVERLAY}, this flag will also impact the
     * insets it adds to those given to the application.
     *
     * @deprecated Use {@link WindowInsets#getInsetsIgnoringVisibility(int)} instead to retrieve
     * insets that don't change when system bars change visibility state.
     */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LAYOUT_STABLE = 0x00000100;
 /**
     * Flag for {@link #setSystemUiVisibility(int)}: View would like its window
     * to be laid out as if it has requested
     * {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}, even if it currently hasn't.  This
     * allows it to avoid artifacts when switching in and out of that mode, at
     * the expense that some of its user interface may be covered by screen
     * decorations when they are shown.  You can perform layout of your inner
     * UI elements to account for the navigation system UI through the
     * {@link #fitSystemWindows(Rect)} method.
     *
     * @deprecated For floating windows, use {@link LayoutParams#setFitInsetsTypes(int)} with
     * {@link Type#navigationBars()}. For non-floating windows that fill the screen, call
     * {@link Window#setDecorFitsSystemWindows(boolean)} with {@code false}.
     */
    public static final int SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION = 0x00000200;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View would like its window
     * to be laid out as if it has requested
     * {@link #SYSTEM_UI_FLAG_FULLSCREEN}, even if it currently hasn't.  This
     * allows it to avoid artifacts when switching in and out of that mode, at
     * the expense that some of its user interface may be covered by screen
     * decorations when they are shown.  You can perform layout of your inner
     * UI elements to account for non-fullscreen system UI through the
     * {@link #fitSystemWindows(Rect)} method.
     *
     * <p>Note: on displays that have a {@link DisplayCutout}, the window may still be placed
     *  differently than if {@link #SYSTEM_UI_FLAG_FULLSCREEN} was set, if the
     *  window's {@link WindowManager.LayoutParams#layoutInDisplayCutoutMode
     *  layoutInDisplayCutoutMode} is
     *  {@link WindowManager.LayoutParams#LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
     *  LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT}. To avoid this, use either of the other modes.
     *
     * @see WindowManager.LayoutParams#layoutInDisplayCutoutMode
     * @see WindowManager.LayoutParams#LAYOUT_IN_DISPLAY_CUTOUT_MODE_DEFAULT
     * @see WindowManager.LayoutParams#LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
     * @see WindowManager.LayoutParams#LAYOUT_IN_DISPLAY_CUTOUT_MODE_NEVER
     *
     * @deprecated For floating windows, use {@link LayoutParams#setFitInsetsTypes(int)} with
     * {@link Type#statusBars()} ()}. For non-floating windows that fill the screen, call
     * {@link Window#setDecorFitsSystemWindows(boolean)} with {@code false}.
     */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN = 0x00000400;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View would like to remain interactive when
     * hiding the navigation bar with {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}.  If this flag is
     * not set, {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION} will be force cleared by the system on any
     * user interaction.
     * <p>Since this flag is a modifier for {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}, it only
     * has an effect when used in combination with that flag.</p>
     *
     * @deprecated Use {@link WindowInsetsController#BEHAVIOR_DEFAULT} instead.
     */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_IMMERSIVE = 0x00000800;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: View would like to remain interactive when
     * hiding the status bar with {@link #SYSTEM_UI_FLAG_FULLSCREEN} and/or hiding the navigation
     * bar with {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}.  Use this flag to create an immersive
     * experience while also hiding the system bars.  If this flag is not set,
     * {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION} will be force cleared by the system on any user
     * interaction, and {@link #SYSTEM_UI_FLAG_FULLSCREEN} will be force-cleared by the system
     * if the user swipes from the top of the screen.
     * <p>When system bars are hidden in immersive mode, they can be revealed temporarily with
     * system gestures, such as swiping from the top of the screen.  These transient system bars
     * will overlay app's content, may have some degree of transparency, and will automatically
     * hide after a short timeout.
     * </p><p>Since this flag is a modifier for {@link #SYSTEM_UI_FLAG_FULLSCREEN} and
     * {@link #SYSTEM_UI_FLAG_HIDE_NAVIGATION}, it only has an effect when used in combination
     * with one or both of those flags.</p>
     *
     * @deprecated Use {@link WindowInsetsController#BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE} instead.
     */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_IMMERSIVE_STICKY = 0x00001000;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: Requests the status bar to draw in a mode that
     * is compatible with light status bar backgrounds.
     *
     * <p>For this to take effect, the window must request
     * {@link android.view.WindowManager.LayoutParams#FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
     *         FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS} but not
     * {@link android.view.WindowManager.LayoutParams#FLAG_TRANSLUCENT_STATUS
     *         FLAG_TRANSLUCENT_STATUS}.
     *
     * @see android.R.attr#windowLightStatusBar
     * @deprecated Use {@link WindowInsetsController#APPEARANCE_LIGHT_STATUS_BARS} instead.
     */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LIGHT_STATUS_BAR = 0x00002000;

    /**
     * This flag was previously used for a private API. DO NOT reuse it for a public API as it might
     * trigger undefined behavior on older platforms with apps compiled against a new SDK.
     */
    private static final int SYSTEM_UI_RESERVED_LEGACY1 = 0x00004000;

    /**
     * This flag was previously used for a private API. DO NOT reuse it for a public API as it might
     * trigger undefined behavior on older platforms with apps compiled against a new SDK.
     */
    private static final int SYSTEM_UI_RESERVED_LEGACY2 = 0x00010000;

    /**
     * Flag for {@link #setSystemUiVisibility(int)}: Requests the navigation bar to draw in a mode
     * that is compatible with light navigation bar backgrounds.
     *
     * <p>For this to take effect, the window must request
     * {@link android.view.WindowManager.LayoutParams#FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS
     *         FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS} but not
     * {@link android.view.WindowManager.LayoutParams#FLAG_TRANSLUCENT_NAVIGATION
     *         FLAG_TRANSLUCENT_NAVIGATION}.
     *
     * @see android.R.attr#windowLightNavigationBar
     * @deprecated Use {@link WindowInsetsController#APPEARANCE_LIGHT_NAVIGATION_BARS} instead.
     */
    @Deprecated
    public static final int SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR = 0x00000010;

    /**
     * @deprecated Use {@link #SYSTEM_UI_FLAG_LOW_PROFILE} instead.
     */
    @Deprecated
    public static final int STATUS_BAR_HIDDEN = SYSTEM_UI_FLAG_LOW_PROFILE;

    /**
     * @deprecated Use {@link #SYSTEM_UI_FLAG_VISIBLE} instead.
     */
    @Deprecated
    public static final int STATUS_BAR_VISIBLE = SYSTEM_UI_FLAG_VISIBLE;

    /**
     * @hide
     *
     * NOTE: This flag may only be used in subtreeSystemUiVisibility. It is masked
     * out of the public fields to keep the undefined bits out of the developer's way.
     *
     * Flag to make the status bar not expandable.  Unless you also
     * set {@link #STATUS_BAR_DISABLE_NOTIFICATION_ICONS}, new notifications will continue to show.
     */
    @UnsupportedAppUsage(maxTargetSdk = Build.VERSION_CODES.R, trackingBug = 170729553)
    public static final int STATUS_BAR_DISABLE_EXPAND = 0x00010000;

    /**
     * @hide
     *
     * NOTE: This flag may only be used in subtreeSystemUiVisibility. It is masked
     * out of the public fields to keep the undefined bits out of the developer's way.
     *
     * Flag to hide notification icons and scrolling ticker text.
     */
    public static final int STATUS_BAR_DISABLE_NOTIFICATION_ICONS = 0x00020000;

    /**
     * @hide
     *
     * NOTE: This flag may only be used in subtreeSystemUiVisibility. It is masked
     * out of the public fields to keep the undefined bits out of the developer's way.
     *
     * Flag to disable incoming notification alerts.  This will not block
     * icons, but it will block sound, vibrating and other visual or aural notifications.
     */
    public static final int STATUS_BAR_DISABLE_NOTIFICATION_ALERTS = 0x00040000;

    /**
     * @hide
     *
     * NOTE: This flag may only be used in subtreeSystemUiVisibility. It is masked
     * out of the public fields to keep the undefined bits out of the developer's way.
     *
     * Flag to hide only the scrolling ticker.  Note that
     * {@link #STATUS_BAR_DISABLE_NOTIFICATION_ICONS} implies
     * {@link #STATUS_BAR_DISABLE_NOTIFICATION_TICKER}.
     */
    public static final int STATUS_BAR_DISABLE_NOTIFICATION_TICKER = 0x00080000;

    /**
     * @hide
     *
     * NOTE: This flag may only be used in subtreeSystemUiVisibility. It is masked
     * out of the public fields to keep the undefined bits out of the developer's way.
     *
     * Flag to hide the center system info area.
     */
    public static final int STATUS_BAR_DISABLE_SYSTEM_INFO = 0x00100000;
