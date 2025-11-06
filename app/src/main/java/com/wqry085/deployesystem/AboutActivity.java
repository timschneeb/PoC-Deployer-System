package com.wqry085.deployesystem;

import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.drakeet.about.AbsAboutActivity;
import com.drakeet.about.Card;
import com.drakeet.about.Category;
import com.drakeet.about.Contributor;
import com.drakeet.about.License;

import java.util.List;

public class AboutActivity extends AbsAboutActivity {

    @Override
    protected void onCreateHeader(@NonNull ImageView icon,
                                  @NonNull TextView slogan,
                                  @NonNull TextView version) {
        // 应用图标
        icon.setImageResource(R.mipmap.ic_launcher);

        // 版本号
        version.setText("v1.4-alpha02");
    }

    @Override
    protected void onItemsCreated(@NonNull List<Object> items) {
        // 关于应用
        items.add(new Category(getString(R.string.about_app)));
        items.add(new Card(getString(R.string.tool_description)));
        items.add(new Card(getString(R.string.feedback_message)));

        // 开发者信息
        items.add(new Category(getString(R.string.developer)));
        items.add(new Contributor(
                R.drawable.ic_wa,
                "wqry085",
                getString(R.string.developer_home),
                "http://www.coolapk.com/u/21820733"
        ));

        items.add(new Category(getString(R.string.contributor)));
        items.add(new Contributor(
                R.drawable.ic_launcher_foreground,
                "timschneeb",
                getString(R.string.developer_home),
                "https://github.com/timschneeb"
        ));

        // 项目信息
        items.add(new Category(getString(R.string.project)));
        items.add(new License(
                "PoC-Deployer-System",
                "wqry085",
                License.MIT,
                "https://codeberg.org/wqry085/PoC-Deployer-System"
        ));
    }
}