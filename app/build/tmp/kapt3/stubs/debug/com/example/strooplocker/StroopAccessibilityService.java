package com.example.strooplocker;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\"\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\u0002\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\b\u0005\u0018\u0000 \r2\u00020\u0001:\u0001\rB\u0005\u00a2\u0006\u0002\u0010\u0002J\u000e\u0010\u0003\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006J\u0012\u0010\u0007\u001a\u00020\u00042\b\u0010\b\u001a\u0004\u0018\u00010\tH\u0016J\b\u0010\n\u001a\u00020\u0004H\u0016J\b\u0010\u000b\u001a\u00020\u0004H\u0014J\u000e\u0010\f\u001a\u00020\u00042\u0006\u0010\u0005\u001a\u00020\u0006\u00a8\u0006\u000e"}, d2 = {"Lcom/example/strooplocker/StroopAccessibilityService;", "Landroid/accessibilityservice/AccessibilityService;", "()V", "markChallengeCompleted", "", "packageName", "", "onAccessibilityEvent", "event", "Landroid/view/accessibility/AccessibilityEvent;", "onInterrupt", "onServiceConnected", "resetChallenge", "Companion", "app_debug"})
public final class StroopAccessibilityService extends android.accessibilityservice.AccessibilityService {
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String TAG = "StroopAccessibilityService_DEBUG";
    @org.jetbrains.annotations.NotNull
    public static final java.lang.String EXTRA_LOCKED_PACKAGE = "TARGET_PACKAGE";
    private static final long CHALLENGE_TIMEOUT_MS = 30000L;
    @kotlin.jvm.Volatile
    private static volatile boolean challengeInProgress = false;
    @kotlin.jvm.Volatile
    @org.jetbrains.annotations.Nullable
    private static volatile java.lang.String pendingLockedPackage;
    @kotlin.jvm.Volatile
    private static volatile long lastChallengeTime = 0L;
    @org.jetbrains.annotations.NotNull
    private static final java.util.Set<java.lang.String> completedChallenges = null;
    @org.jetbrains.annotations.NotNull
    public static final com.example.strooplocker.StroopAccessibilityService.Companion Companion = null;
    
    public StroopAccessibilityService() {
        super();
    }
    
    @java.lang.Override
    protected void onServiceConnected() {
    }
    
    @java.lang.Override
    public void onAccessibilityEvent(@org.jetbrains.annotations.Nullable
    android.view.accessibility.AccessibilityEvent event) {
    }
    
    public final void markChallengeCompleted(@org.jetbrains.annotations.NotNull
    java.lang.String packageName) {
    }
    
    public final void resetChallenge(@org.jetbrains.annotations.NotNull
    java.lang.String packageName) {
    }
    
    @java.lang.Override
    public void onInterrupt() {
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000*\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\t\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010\u000b\n\u0002\b\u0005\n\u0002\u0010#\n\u0002\b\u0007\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0005\u001a\u00020\u0006X\u0086T\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0007\u001a\u00020\u0006X\u0082T\u00a2\u0006\u0002\n\u0000R\u001a\u0010\b\u001a\u00020\tX\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\n\u0010\u000b\"\u0004\b\f\u0010\rR\u0014\u0010\u000e\u001a\b\u0012\u0004\u0012\u00020\u00060\u000fX\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u000e\u0010\u0010\u001a\u00020\u0004X\u0082\u000e\u00a2\u0006\u0002\n\u0000R\u001c\u0010\u0011\u001a\u0004\u0018\u00010\u0006X\u0086\u000e\u00a2\u0006\u000e\n\u0000\u001a\u0004\b\u0012\u0010\u0013\"\u0004\b\u0014\u0010\u0015\u00a8\u0006\u0016"}, d2 = {"Lcom/example/strooplocker/StroopAccessibilityService$Companion;", "", "()V", "CHALLENGE_TIMEOUT_MS", "", "EXTRA_LOCKED_PACKAGE", "", "TAG", "challengeInProgress", "", "getChallengeInProgress", "()Z", "setChallengeInProgress", "(Z)V", "completedChallenges", "", "lastChallengeTime", "pendingLockedPackage", "getPendingLockedPackage", "()Ljava/lang/String;", "setPendingLockedPackage", "(Ljava/lang/String;)V", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
        
        public final boolean getChallengeInProgress() {
            return false;
        }
        
        public final void setChallengeInProgress(boolean p0) {
        }
        
        @org.jetbrains.annotations.Nullable
        public final java.lang.String getPendingLockedPackage() {
            return null;
        }
        
        public final void setPendingLockedPackage(@org.jetbrains.annotations.Nullable
        java.lang.String p0) {
        }
    }
}