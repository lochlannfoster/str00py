package com.example.strooplocker;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\u0018\u0000 \u00122\u00020\u0001:\u0002\u0011\u0012B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u0007\u001a\u00020\bH\u0002J\u0018\u0010\t\u001a\u00020\b2\u0006\u0010\n\u001a\u00020\u000b2\u0006\u0010\f\u001a\u00020\rH\u0002J\u0012\u0010\u000e\u001a\u00020\b2\b\u0010\u000f\u001a\u0004\u0018\u00010\u0010H\u0014R\u0012\u0010\u0003\u001a\u00060\u0004R\u00020\u0000X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0013"}, d2 = {"Lcom/example/strooplocker/SelectAppsActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "appsAdapter", "Lcom/example/strooplocker/SelectAppsActivity$AppsAdapter;", "recyclerView", "Landroidx/recyclerview/widget/RecyclerView;", "loadInstalledApps", "", "onAppSelected", "app", "Landroid/content/pm/ApplicationInfo;", "isCurrentlyLocked", "", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "AppsAdapter", "Companion", "app_debug"})
public final class SelectAppsActivity extends androidx.appcompat.app.AppCompatActivity {
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String TAG = "SelectAppsActivity_DEBUG";
    private androidx.recyclerview.widget.RecyclerView recyclerView;
    private com.example.strooplocker.SelectAppsActivity.AppsAdapter appsAdapter;
    @org.jetbrains.annotations.NotNull
    public static final com.example.strooplocker.SelectAppsActivity.Companion Companion = null;
    
    public SelectAppsActivity() {
        super();
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    private final void loadInstalledApps() {
    }
    
    private final void onAppSelected(android.content.pm.ApplicationInfo app, boolean isCurrentlyLocked) {
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000F\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\"\n\u0002\u0010\u000e\n\u0000\n\u0002\u0018\u0002\n\u0002\u0010\u000b\n\u0002\u0010\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0086\u0004\u0018\u00002\u0010\u0012\f\u0012\n0\u0002R\u00060\u0000R\u00020\u00030\u0001:\u0001\u001aB;\u0012\f\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005\u0012\f\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\b\u0012\u0018\u0010\n\u001a\u0014\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\r0\u000b\u00a2\u0006\u0002\u0010\u000eJ\b\u0010\u000f\u001a\u00020\u0010H\u0016J \u0010\u0011\u001a\u00020\r2\u000e\u0010\u0012\u001a\n0\u0002R\u00060\u0000R\u00020\u00032\u0006\u0010\u0013\u001a\u00020\u0010H\u0016J \u0010\u0014\u001a\n0\u0002R\u00060\u0000R\u00020\u00032\u0006\u0010\u0015\u001a\u00020\u00162\u0006\u0010\u0017\u001a\u00020\u0010H\u0016J\u0014\u0010\u0018\u001a\u00020\r2\f\u0010\u0019\u001a\b\u0012\u0004\u0012\u00020\t0\bR\u0014\u0010\u0004\u001a\b\u0012\u0004\u0012\u00020\u00060\u0005X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0014\u0010\u0007\u001a\b\u0012\u0004\u0012\u00020\t0\bX\u0082\u000e\u00a2\u0006\u0002\n\u0000R \u0010\n\u001a\u0014\u0012\u0004\u0012\u00020\u0006\u0012\u0004\u0012\u00020\f\u0012\u0004\u0012\u00020\r0\u000bX\u0082\u0004\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u001b"}, d2 = {"Lcom/example/strooplocker/SelectAppsActivity$AppsAdapter;", "Landroidx/recyclerview/widget/RecyclerView$Adapter;", "Lcom/example/strooplocker/SelectAppsActivity$AppsAdapter$AppViewHolder;", "Lcom/example/strooplocker/SelectAppsActivity;", "apps", "", "Landroid/content/pm/ApplicationInfo;", "lockedApps", "", "", "onAppClick", "Lkotlin/Function2;", "", "", "(Lcom/example/strooplocker/SelectAppsActivity;Ljava/util/List;Ljava/util/Set;Lkotlin/jvm/functions/Function2;)V", "getItemCount", "", "onBindViewHolder", "holder", "position", "onCreateViewHolder", "parent", "Landroid/view/ViewGroup;", "viewType", "updateLockedApps", "newLockedApps", "AppViewHolder", "app_debug"})
    public final class AppsAdapter extends androidx.recyclerview.widget.RecyclerView.Adapter<com.example.strooplocker.SelectAppsActivity.AppsAdapter.AppViewHolder> {
        @org.jetbrains.annotations.NotNull
        private final java.util.List<android.content.pm.ApplicationInfo> apps = null;
        @org.jetbrains.annotations.NotNull
        private java.util.Set<java.lang.String> lockedApps;
        @org.jetbrains.annotations.NotNull
        private final kotlin.jvm.functions.Function2<android.content.pm.ApplicationInfo, java.lang.Boolean, kotlin.Unit> onAppClick = null;
        
        public AppsAdapter(@org.jetbrains.annotations.NotNull
        java.util.List<? extends android.content.pm.ApplicationInfo> apps, @org.jetbrains.annotations.NotNull
        java.util.Set<java.lang.String> lockedApps, @org.jetbrains.annotations.NotNull
        kotlin.jvm.functions.Function2<? super android.content.pm.ApplicationInfo, ? super java.lang.Boolean, kotlin.Unit> onAppClick) {
            super();
        }
        
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public com.example.strooplocker.SelectAppsActivity.AppsAdapter.AppViewHolder onCreateViewHolder(@org.jetbrains.annotations.NotNull
        android.view.ViewGroup parent, int viewType) {
            return null;
        }
        
        @java.lang.Override
        public void onBindViewHolder(@org.jetbrains.annotations.NotNull
        com.example.strooplocker.SelectAppsActivity.AppsAdapter.AppViewHolder holder, int position) {
        }
        
        @java.lang.Override
        public int getItemCount() {
            return 0;
        }
        
        public final void updateLockedApps(@org.jetbrains.annotations.NotNull
        java.util.Set<java.lang.String> newLockedApps) {
        }
        
        @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0018\u0002\n\u0002\b\u0005\b\u0086\u0004\u0018\u00002\u00020\u0001B\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004R\u0011\u0010\u0005\u001a\u00020\u0006\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\t\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000b\u0010\fR\u0011\u0010\r\u001a\u00020\n\u00a2\u0006\b\n\u0000\u001a\u0004\b\u000e\u0010\f\u00a8\u0006\u000f"}, d2 = {"Lcom/example/strooplocker/SelectAppsActivity$AppsAdapter$AppViewHolder;", "Landroidx/recyclerview/widget/RecyclerView$ViewHolder;", "view", "Landroid/view/View;", "(Lcom/example/strooplocker/SelectAppsActivity$AppsAdapter;Landroid/view/View;)V", "appIcon", "Landroid/widget/ImageView;", "getAppIcon", "()Landroid/widget/ImageView;", "appName", "Landroid/widget/TextView;", "getAppName", "()Landroid/widget/TextView;", "lockStatus", "getLockStatus", "app_debug"})
        public final class AppViewHolder extends androidx.recyclerview.widget.RecyclerView.ViewHolder {
            @org.jetbrains.annotations.NotNull
            private final android.widget.ImageView appIcon = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView appName = null;
            @org.jetbrains.annotations.NotNull
            private final android.widget.TextView lockStatus = null;
            
            public AppViewHolder(@org.jetbrains.annotations.NotNull
            android.view.View view) {
                super(null);
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.ImageView getAppIcon() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getAppName() {
                return null;
            }
            
            @org.jetbrains.annotations.NotNull
            public final android.widget.TextView getLockStatus() {
                return null;
            }
        }
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/example/strooplocker/SelectAppsActivity$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}