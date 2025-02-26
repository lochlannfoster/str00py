package com.example.strooplocker;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u00006\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010 \n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0002\b\u00c6\u0002\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J!\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004H\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\nJ\u001f\u0010\u000b\u001a\b\u0012\u0004\u0012\u00020\u00040\f2\u0006\u0010\u0007\u001a\u00020\bH\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\rJ\u0010\u0010\u000e\u001a\u00020\u000f2\u0006\u0010\u0007\u001a\u00020\bH\u0002J!\u0010\u0010\u001a\u00020\u00112\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004H\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\nJ!\u0010\u0012\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\b2\u0006\u0010\t\u001a\u00020\u0004H\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\nR\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u0013"}, d2 = {"Lcom/example/strooplocker/LockManager;", "", "()V", "TAG", "", "addLockedApp", "", "context", "Landroid/content/Context;", "packageName", "(Landroid/content/Context;Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getLockedApps", "", "(Landroid/content/Context;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getRepository", "Lcom/example/strooplocker/data/LockedAppsRepository;", "isAppLocked", "", "removeLockedApp", "app_debug"})
public final class LockManager {
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String TAG = "LockManager_DEBUG";
    @org.jetbrains.annotations.NotNull
    public static final com.example.strooplocker.LockManager INSTANCE = null;
    
    private LockManager() {
        super();
    }
    
    /**
     * Adds an app to the locked apps list
     */
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object addLockedApp(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    java.lang.String packageName, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Removes an app from the locked apps list
     */
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object removeLockedApp(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    java.lang.String packageName, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super kotlin.Unit> $completion) {
        return null;
    }
    
    /**
     * Gets all currently locked apps
     */
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object getLockedApps(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.util.List<java.lang.String>> $completion) {
        return null;
    }
    
    /**
     * Checks if a specific app is locked
     */
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object isAppLocked(@org.jetbrains.annotations.NotNull
    android.content.Context context, @org.jetbrains.annotations.NotNull
    java.lang.String packageName, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.lang.Boolean> $completion) {
        return null;
    }
    
    /**
     * Helper to get repository instance
     */
    private final com.example.strooplocker.data.LockedAppsRepository getRepository(android.content.Context context) {
        return null;
    }
}