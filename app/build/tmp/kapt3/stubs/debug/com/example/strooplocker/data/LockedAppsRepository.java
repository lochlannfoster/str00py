package com.example.strooplocker.data;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0002\n\u0002\u0010\b\n\u0000\n\u0002\u0010\u000e\n\u0002\b\u0002\n\u0002\u0010 \n\u0002\b\u0004\u0018\u0000 \u000e2\u00020\u0001:\u0001\u000eB\r\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\u0002\u0010\u0004J\u0019\u0010\u0005\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\tJ\u0017\u0010\n\u001a\b\u0012\u0004\u0012\u00020\b0\u000bH\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\fJ\u0019\u0010\r\u001a\u00020\u00062\u0006\u0010\u0007\u001a\u00020\bH\u0086@\u00f8\u0001\u0000\u00a2\u0006\u0002\u0010\tR\u000e\u0010\u0002\u001a\u00020\u0003X\u0082\u0004\u00a2\u0006\u0002\n\u0000\u0082\u0002\u0004\n\u0002\b\u0019\u00a8\u0006\u000f"}, d2 = {"Lcom/example/strooplocker/data/LockedAppsRepository;", "", "dao", "Lcom/example/strooplocker/data/LockedAppDao;", "(Lcom/example/strooplocker/data/LockedAppDao;)V", "addLockedApp", "", "packageName", "", "(Ljava/lang/String;Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "getAllLockedApps", "", "(Lkotlin/coroutines/Continuation;)Ljava/lang/Object;", "removeLockedApp", "Companion", "app_debug"})
public final class LockedAppsRepository {
    @org.jetbrains.annotations.NotNull
    private final com.example.strooplocker.data.LockedAppDao dao = null;
    @org.jetbrains.annotations.NotNull
    private static final java.lang.String TAG = "LockedAppsRepository_DEBUG";
    @org.jetbrains.annotations.NotNull
    public static final com.example.strooplocker.data.LockedAppsRepository.Companion Companion = null;
    
    public LockedAppsRepository(@org.jetbrains.annotations.NotNull
    com.example.strooplocker.data.LockedAppDao dao) {
        super();
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object getAllLockedApps(@org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.util.List<java.lang.String>> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object addLockedApp(@org.jetbrains.annotations.NotNull
    java.lang.String packageName, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    @org.jetbrains.annotations.Nullable
    public final java.lang.Object removeLockedApp(@org.jetbrains.annotations.NotNull
    java.lang.String packageName, @org.jetbrains.annotations.NotNull
    kotlin.coroutines.Continuation<? super java.lang.Integer> $completion) {
        return null;
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\u0012\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0010\u000e\n\u0000\b\u0086\u0003\u0018\u00002\u00020\u0001B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002R\u000e\u0010\u0003\u001a\u00020\u0004X\u0082T\u00a2\u0006\u0002\n\u0000\u00a8\u0006\u0005"}, d2 = {"Lcom/example/strooplocker/data/LockedAppsRepository$Companion;", "", "()V", "TAG", "", "app_debug"})
    public static final class Companion {
        
        private Companion() {
            super();
        }
    }
}