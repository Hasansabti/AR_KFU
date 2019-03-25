// Generated code from Butter Knife. Do not modify!
package arnavigation.appsan.com.myapplication;

import android.support.annotation.CallSuper;
import android.support.annotation.UiThread;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import butterknife.Unbinder;
import butterknife.internal.Utils;
import java.lang.IllegalStateException;
import java.lang.Override;

public class NavActivity_ViewBinding implements Unbinder {
  private NavActivity target;

  @UiThread
  public NavActivity_ViewBinding(NavActivity target) {
    this(target, target.getWindow().getDecorView());
  }

  @UiThread
  public NavActivity_ViewBinding(NavActivity target, View source) {
    this.target = target;

    target.sourcePickBtn = Utils.findRequiredViewAsType(source, R.id.source_pick_btn, "field 'sourcePickBtn'", Button.class);
    target.destPickBtn = Utils.findRequiredViewAsType(source, R.id.dest_pick_btn, "field 'destPickBtn'", Button.class);
    target.navStartBtn = Utils.findRequiredViewAsType(source, R.id.nav_start_btn, "field 'navStartBtn'", Button.class);
    target.sourceResultText = Utils.findRequiredViewAsType(source, R.id.source_result_text, "field 'sourceResultText'", TextView.class);
    target.destResultText = Utils.findRequiredViewAsType(source, R.id.dest_result_text, "field 'destResultText'", TextView.class);
  }

  @Override
  @CallSuper
  public void unbind() {
    NavActivity target = this.target;
    if (target == null) throw new IllegalStateException("Bindings already cleared.");
    this.target = null;

    target.sourcePickBtn = null;
    target.destPickBtn = null;
    target.navStartBtn = null;
    target.sourceResultText = null;
    target.destResultText = null;
  }
}
