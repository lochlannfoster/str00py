package com.example.strooplocker.data;

@kotlin.Metadata(mv = {1, 8, 0}, k = 1, xi = 48, d1 = {"\u0000\u001e\n\u0002\u0018\u0002\n\u0002\u0010\u0000\n\u0000\n\u0002\u0010\u0002\n\u0000\n\u0002\u0018\u0002\n\u0000\n\u0002\u0010 \n\u0002\b\u0002\bg\u0018\u00002\u00020\u0001J\u0010\u0010\u0002\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\'J\u000e\u0010\u0006\u001a\b\u0012\u0004\u0012\u00020\u00050\u0007H\'J\u0010\u0010\b\u001a\u00020\u00032\u0006\u0010\u0004\u001a\u00020\u0005H\'\u00a8\u0006\t"}, d2 = {"Lcom/example/strooplocker/data/LockedAppDao;", "", "deleteLockedApp", "", "lockedApp", "Lcom/example/strooplocker/data/LockedApp;", "getAllLockedApps", "", "insertLockedApp", "app_debug"})
@androidx.room.Dao
public abstract interface LockedAppDao {
    
    @androidx.room.Query(value = "SELECT * FROM locked_apps")
    @org.jetbrains.annotations.NotNull
    public abstract java.util.List<com.example.strooplocker.data.LockedApp> getAllLockedApps();
    
    @androidx.room.Insert(onConflict = 5)
    public abstract void insertLockedApp(@org.jetbrains.annotations.NotNull
    com.example.strooplocker.data.LockedApp lockedApp);
    
    @androidx.room.Delete
    public abstract void deleteLockedApp(@org.jetbrains.annotations.NotNull
    com.example.strooplocker.data.LockedApp lockedApp);
}