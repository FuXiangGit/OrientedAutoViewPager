# OrientedAutoViewPager
自定义水平、垂直自动滚动的自定义Viewpager </br>
京东精选卡片翻转效果，淘宝广告卡片切换效果，自动翻转效果</br>
效果图如下 : </br>
![水平效果](https://github.com/bxfx111/OrientedAutoViewPager/blob/master/gif/hor.gif)

![垂直效果](https://github.com/bxfx111/OrientedAutoViewPager/blob/master/gif/vi.gif)

## 简单使用方式如下（详细使用请看代码）：
###布局如下
```
<oriauto.fyx.orientedautolib.OrientedAutoViewPager
        android:id="@+id/src_view_pager"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
```
###第一步，叠图计算,获取循环数据
```
public List<Fragment> getLoopData(List<Fragment> cardFragments) {
        if (cardFragments != null) {
            //====================叠图计算方法=============
            fragmentList.clear();
            //加上末位放在第一个位置，为了能左滑
            fragmentList.add(CardFragment.newInstance(cardFragments.size() - 1));
            //加上标准展示数据
            for (int i = 0; i < cardFragments.size(); i++) {
                fragmentList.add(CardFragment.newInstance(i));
            }
            //重复加一遍为了末位图后有叠图，否则后几位叠图不全
            for (int i = 0; i < cardFragments.size(); i++) {
                fragmentList.add(CardFragment.newInstance(i));
            }
            return fragmentList;
            //====================叠图计算方法=============
        }
        return fragmentList;
    }
```
###第二步，如果循环设置为1，启动自动滚动播放，设置自动滚动播放间隔时间,默认两秒
```
 viewPager.setCurrentItem(1);//如果循环设置为1
 viewPager.setAutoLoop(true);//启动自动滚动播放
 viewPager.setAutoScroDelayMillis(3000);//设置自动滚动播放间隔时间,默认两秒
```
###第三步，配合叠图计算,跳转对应位置这句话要放在最上面,放在OnPageChangeListener里也行
```
viewPager.addOnPageChangeListener(new ViewPager.SimpleOnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //配合叠图计算,跳转对应位置这句话要放在最上面,放在OnPageChangeListener里也行
                viewPager.setPageScrolled(position, positionOffset, positionOffsetPixels);
                // TODO: 2017/12/4 下面做要做的工作
            }
        });
 ```
或者</br>
```
 viewPager.addOnPageChangeListener(new ViewPager.OnPageChangeListener() {
            @Override
            public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
                //配合叠图计算,跳转对应位置这句话要放在最上面,放在放在SimpleOnPageChangeListener里也行
                viewPager.setPageScrolled(position, positionOffset, positionOffsetPixels);
                // TODO: 2017/12/4 下面做要做的工作
            }

            @Override
            public void onPageSelected(int position) {

            }

            @Override
            public void onPageScrollStateChanged(int state) {

            }
        });
```
翻转特效</br>
```
//垂直翻滚，还是水平翻滚,默认水平翻滚
//        viewPager.setOrientation(OrientedAutoViewPager.Orientation.VERTICAL);
//        viewPager.setPageTransformer(true, new VerticalStackTransformer(this, cardFragments.size()));
        //水平翻滚
        viewPager.setPageTransformer(true, new HorizontalStackTransformer(this, 10, cardFragments.size()));
```
