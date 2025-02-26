package com.example.strooplocker;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000n\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010!\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010$\n\u0002\b\u0005\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0002\b\u0006\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\n\u0002\u0010\b\n\u0002\b\u0003\u0018\u0000 /2\u00020\u0001:\u0001/B\u0005\u00a2\u0006\u0002\u0010\u0002J\b\u0010\u001a\u001a\u00020\u001bH\u0002J\b\u0010\u001c\u001a\u00020\u001bH\u0002J\u0018\u0010\u001d\u001a\u00020\u001b2\u0006\u0010\u001e\u001a\u00020\u00052\u0006\u0010\u001f\u001a\u00020\nH\u0002J\u001c\u0010 \u001a\u00020\f2\u0006\u0010!\u001a\u00020\"2\n\u0010#\u001a\u0006\u0012\u0002\b\u00030$H\u0002J\u0012\u0010%\u001a\u00020\u001b2\b\u0010&\u001a\u0004\u0018\u00010\'H\u0014J\b\u0010(\u001a\u00020\u001bH\u0014J\b\u0010)\u001a\u00020\u001bH\u0014J\b\u0010*\u001a\u00020\u001bH\u0014J \u0010+\u001a\u00020\u001b2\u0006\u0010\u001e\u001a\u00020\u00052\u0006\u0010,\u001a\u00020-2\u0006\u0010.\u001a\u00020-H\u0002R\u0014\u0010\u0003\u001a\b\u0012\u0004\u0012\u00020\u00050\u0004X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0006\u001a\u00020\u0007X\u0082.\u00a2\u0006\u0002\n\u0000R\u0014\u0010\b\u001a\b\u0012\u0004\u0012\u00020\n0\tX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u000b\u001a\u00020\fX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\r\u001a\u00020\u000eX\u0082.\u00a2\u0006\u0002\n\u0000R\u001a\u0010\u000f\u001a\u000e\u0012\u0004\u0012\u00020\n\u0012\u0004\u0012\u00020\n0\u0010X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0011\u001a\u00020\u0005X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0012\u001a\u00020\u0005X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0013\u001a\u00020\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u0010\u0010\u0014\u001a\u0004\u0018\u00010\nX\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0015\u001a\u00020\u0016X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0017\u001a\u00020\u0018X\u0082.\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0019\u001a\u00020\u0005X\u0082.\u00a2\u0006\u0002\n\u0000\u00a8\u00060"}, d2 = {"Lcom/example/strooplocker/StroopLockActivity;", "Landroidx/appcompat/app/AppCompatActivity;", "()V", "answerButtons", "", "Landroid/widget/Button;", "answerGrid", "Landroid/widget/GridLayout;", "availablePool", "", "", "buttonCooldownActive", "", "challengeText", "Landroid/widget/TextView;", "colorMap", "", "enableAccessibilityButton", "exitButton", "expectedAnswer", "lockedPackageToLaunch", "repository", "Lcom/example/strooplocker/data/LockedAppsRepository;", "rootLayout", "Landroidx/constraintlayout/widget/ConstraintLayout;", "selectAppButton", "applyChallenge", "", "generateChallengeButtons", "handleButtonClick", "button", "selectedColor", "isAccessibilityServiceEnabled", "context", "Landroid/content/Context;", "serviceClass", "Ljava/lang/Class;", "onCreate", "savedInstanceState", "Landroid/os/Bundle;", "onPause", "onResume", "onUserLeaveHint", "styleMenuButton", "backgroundColor", "", "textColor", "Companion", "app_debug"})
public final class StroopLockActivity extends androidx.appcompat.app.AppCompatActivity {
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String TAG = "StroopLockActivity";
    private static final int REQUEST_CODE_PICK_APP = 2001;
    private com.example.strooplocker.data.LockedAppsRepository repository;
    private android.widget.TextView challengeText;
    private android.widget.GridLayout answerGrid;
    private androidx.constraintlayout.widget.ConstraintLayout rootLayout;
    private android.widget.Button exitButton;
    private android.widget.Button selectAppButton;
    private android.widget.Button enableAccessibilityButton;
    private java.util.List<android.widget.Button> answerButtons;
    @org.jetbrains.annotations.NotNull
    private java.lang.String expectedAnswer = "";
    @org.jetbrains.annotations.NotNull
    private final java.util.Map<java.lang.String, java.lang.String> colorMap = null;
    @org.jetbrains.annotations.NotNull
    private final java.util.List<java.lang.String> availablePool = null;
    @kotlin.jvm.Volatile
    private volatile boolean buttonCooldownActive = false;
    @org.jetbrains.annotations.Nullable
    private java.lang.String lockedPackageToLaunch;
    @org.jetbrains.annotations.NotNull
    public static final com.example.strooplocker.StroopLockActivity.Companion Companion = null;
    
    public StroopLockActivity() {
        super();
    }
    
    @java.lang.Override
    protected void onCreate(@org.jetbrains.annotations.Nullable
    android.os.Bundle savedInstanceState) {
    }
    
    @java.lang.Override
    protected void onUserLeaveHint() {
    }
    
    @java.lang.Override
    protected void onPause() {
    }
    
    @java.lang.Override
    protected void onResume() {
    }
    
    private final boolean isAccessibilityServiceEnabled(android.content.Context context, java.lang.Class<?> serviceClass) {
        return false;
    }
    
    private final void styleMenuButton(android.widget.Button button, int backgroundColor, int textColor) {
    }
    
    private final void applyChallenge() {
    }
    
    private final void generateChallengeButtons() {
    }
    
    private final void handleButtonClick(android.widget.Button button, java.lang.String selectedColor) {
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\u0018\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0007"}, d2 = {"Lcom/example/strooplocker/StroopLockActivity$Companion;", "", "()V", "REQUEST_CODE_PICK_APP", "", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}