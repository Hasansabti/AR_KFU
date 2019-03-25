// Generated code from Butter Knife. Do not modify!
package arnavigation.appsan.com.myapplication;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import java.lang.IllegalStateException;
import java.lang.Override;

public class ArCamActivity_ViewBinding implements Unbinder {
  private ArCamActivity target;

  @UiThread
  public ArCamActivity_ViewBinding(ArCamActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public ArCamActivity_ViewBinding(ArCamActivity target, View source) {
    this.target = target;

    target.srcDestText = Utils.findRequiredViewAsType(source, R.id.ar_source_dest, "field 'srcDestText'", TextView.class);
    target.dirDistance = Utils.findRequiredViewAsType(source, R.id.ar_dir_distance, "field 'dirDistance'", TextView.class);
    target.dirTime = Utils.findRequiredViewAsType(source, R.id.ar_dir_time, "field 'dirTime'", TextView.class);
    target.speed = Utils.findRequiredViewAsType(source, R.id.speed, "field 'speed'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    ArCamActivity target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.srcDestText = null;
    target.dirDistance = null;
    target.dirTime = null;
    target.speed = null;
  }
}
