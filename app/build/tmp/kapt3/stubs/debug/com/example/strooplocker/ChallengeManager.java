package com.example.strooplocker;

/**
 * Manages the state of Stroop challenges throughout the app.
 */
@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000,\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0002\b\u0002\n\u0002\u0018\u0002\n\u0002\u0018\u0002\n\u0000\n\u0002\u0018\u0002\n\u0002\b\u0003\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\u000b\n\u0002\b\u0005\b\u00c6\u0002\u0018\u00002\u00020\u0001:\u0001\u0011B\u0007\b\u0002\u00a2\u0006\u0002\u0010\u0002J\u0010\u0010\n\u001a\u0004\u0018\u00010\u000b2\u0006\u0010\f\u001a\u00020\rJ\b\u0010\b\u001a\u0004\u0018\u00010\u0005J\u0006\u0010\u000e\u001a\u00020\rJ\u000e\u0010\u000f\u001a\u00020\u00052\u0006\u0010\u0010\u001a\u00020\u000bR\u0016\u0010\u0003\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00050\u0004X\u0082\u0004\u00a2\u0006\u0002\n\u0000R\u0019\u0010\u0006\u001a\n\u0012\u0006\u0012\u0004\u0018\u00010\u00050\u0007\u00a2\u0006\b\n\u0000\u001a\u0004\b\b\u0010\t\u00a8\u0006\u0012"}, d2 = {"Lcom/example/strooplocker/ChallengeManager;", "", "()V", "_currentChallenge", "Lkotlinx/coroutines/flow/MutableStateFlow;", "Lcom/example/strooplocker/ChallengeManager$Challenge;", "currentChallenge", "Lkotlinx/coroutines/flow/StateFlow;", "getCurrentChallenge", "()Lkotlinx/coroutines/flow/StateFlow;", "completeChallenge", "", "success", "", "isChallengeInProgress", "startChallenge", "packageName", "Challenge", "app_debug"})
public final class ChallengeManager {
    @org.jetbrains.annotations.NotNull
    private static final kotlinx.coroutines.flow.MutableStateFlow<com.example.strooplocker.ChallengeManager.Challenge> _currentChallenge = null;
    @org.jetbrains.annotations.NotNull
    private static final kotlinx.coroutines.flow.StateFlow<com.example.strooplocker.ChallengeManager.Challenge> currentChallenge = null;
    @org.jetbrains.annotations.NotNull
    public static final com.example.strooplocker.ChallengeManager INSTANCE = null;
    
    private ChallengeManager() {
        super();
    }
    
    @org.jetbrains.annotations.NotNull
    public final kotlinx.coroutines.flow.StateFlow<com.example.strooplocker.ChallengeManager.Challenge> getCurrentChallenge() {
        return null;
    }
    
    /**
     * Start a new challenge for the given package
     */
    @org.jetbrains.annotations.NotNull
    public final com.example.strooplocker.ChallengeManager.Challenge startChallenge(@org.jetbrains.annotations.NotNull
    java.lang.String packageName) {
        return null;
    }
    
    /**
     * Get the current challenge (if any)
     */
    @org.jetbrains.annotations.Nullable
    public final com.example.strooplocker.ChallengeManager.Challenge getCurrentChallenge() {
        return null;
    }
    
    /**
     * Mark the current challenge as complete
     * @param success Whether the challenge was completed successfully
     * @return The package name if successful, null otherwise
     */
    @org.jetbrains.annotations.Nullable
    public final java.lang.String completeChallenge(boolean success) {
        return null;
    }
    
    /**
     * Check if a challenge is currently in progress
     */
    public final boolean isChallengeInProgress() {
        return false;
    }
    
    @kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000(\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u000e\n\u0000\n\u0002\u0010\t\n\u0002\b\t\n\u0002\u0010\u000b\n\u0002\b\u0002\n\u0002\u0010\b\n\u0002\b\u0002\b\u0086\b\u0018\u00002\u00020\u0001B\u0017\u0012\u0006\u0010\u0002\u001a\u00020\u0003\u0012\b\b\u0002\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\u0002\u0010\u0006J\t\u0010\u000b\u001a\u00020\u0003H\u00c6\u0003J\t\u0010\f\u001a\u00020\u0005H\u00c6\u0003J\u001d\u0010\r\u001a\u00020\u00002\b\b\u0002\u0010\u0002\u001a\u00020\u00032\b\b\u0002\u0010\u0004\u001a\u00020\u0005H\u00c6\u0001J\u0013\u0010\u000e\u001a\u00020\u000f2\b\u0010\u0010\u001a\u0004\u0018\u00010\u0001H\u00d6\u0003J\t\u0010\u0011\u001a\u00020\u0012H\u00d6\u0001J\t\u0010\u0013\u001a\u00020\u0003H\u00d6\u0001R\u0011\u0010\u0002\u001a\u00020\u0003\u00a2\u0006\b\n\u0000\u001a\u0004\b\u0007\u0010\bR\u0011\u0010\u0004\u001a\u00020\u0005\u00a2\u0006\b\n\u0000\u001a\u0004\b\t\u0010\n\u00a8\u0006\u0014"}, d2 = {"Lcom/example/strooplocker/ChallengeManager$Challenge;", "", "lockedPackage", "", "startTime", "", "(Ljava/lang/String;J)V", "getLockedPackage", "()Ljava/lang/String;", "getStartTime", "()J", "component1", "component2", "copy", "equals", "", "other", "hashCode", "", "toString", "app_debug"})
    public static final class Challenge {
        @org.jetbrains.annotations.NotNull
        private final java.lang.String lockedPackage = null;
        private final long startTime = 0L;
        
        public Challenge(@org.jetbrains.annotations.NotNull
        java.lang.String lockedPackage, long startTime) {
            super();
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.lang.String getLockedPackage() {
            return null;
        }
        
        public final long getStartTime() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull
        public final java.lang.String component1() {
            return null;
        }
        
        public final long component2() {
            return 0L;
        }
        
        @org.jetbrains.annotations.NotNull
        public final com.example.strooplocker.ChallengeManager.Challenge copy(@org.jetbrains.annotations.NotNull
        java.lang.String lockedPackage, long startTime) {
            return null;
        }
        
        @java.lang.Override
        public boolean equals(@org.jetbrains.annotations.Nullable
        java.lang.Object other) {
            return false;
        }
        
        @java.lang.Override
        public int hashCode() {
            return 0;
        }
        
        @java.lang.Override
        @org.jetbrains.annotations.NotNull
        public java.lang.String toString() {
            return null;
        }
    }
}