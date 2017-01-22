package in.andres.kandroid.kanboard;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.List;

/**
 * Created by Thomas Andres on 01.01.17.
 */


public class KanboardProject implements Comparable<KanboardProject>, Serializable {
    private int Id;
    private String Name;
    private int OwnerId;
    private String Description;
    private String Identifier;
    private String Token;
    private boolean IsActive;
    private boolean IsPublic;
    private boolean IsPrivate;
    private boolean IsEverybodyAllowed;
    private Date StartDate;
    private Date EndDate;
    private Date LastModified;
    private int NumberActiveTasks;
    private URL ListURL;
    private URL BoardURL;
    private URL CalendarURL;
    private List<KanboardColumn> Columns;
    private List<KanboardCategory> Categories;
    private List<KanboardSwimlane> Swimlanes;
    private List<KanboardTask> ActiveTasks;
    private Dictionary<Integer, Dictionary<Integer, List<KanboardTask>>> GroupedActiveTasks;
    private List<KanboardTask> InactiveTasks;
    private Dictionary<Integer, List<KanboardTask>> GroupedInactiveTasks;
    private List<KanboardTask> OverdueTasks;
    private Dictionary<Integer, List<KanboardTask>> GroupedOverdueTasks;
    private Dictionary<Integer, KanboardTask> TaskHashtable;
    private Dictionary<Integer, KanboardCategory> CategoryHashtable;
    // TODO: add priority values to project details
    // TODO: getProjectById might have additional properties!

    public KanboardProject(@NonNull JSONObject project) throws MalformedURLException {
        this(project, null, null, null, null, null, null);
    }

    public KanboardProject(@NonNull JSONObject project, @Nullable JSONArray columns, @Nullable JSONArray swimlanes,
                           @Nullable JSONArray categories, @Nullable JSONArray activetasks,
                           @Nullable JSONArray inactivetasks, @Nullable JSONArray overduetasks) throws MalformedURLException {
        Id = project.optInt("id");
        Name = project.optString("name");
        OwnerId = project.optInt("owner_id");
        Object desc = project.opt("description");
        Description = desc.equals(null) ? null : desc.toString();
        Identifier = project.optString("identifier");
        Token = project.optString("token");
        IsActive = KanboardAPI.StringToBoolean(project.optString("is_active"));
        IsPublic = KanboardAPI.StringToBoolean(project.optString("is_public"));
        IsPrivate = KanboardAPI.StringToBoolean(project.optString("is_private"));
        IsEverybodyAllowed = KanboardAPI.StringToBoolean(project.optString("is_everybody_allowed"));
        long tmpDate = project.optLong("start_date");
        if (tmpDate > 0)
            StartDate = new Date(tmpDate * 1000);
        else
            StartDate = null;
        tmpDate = project.optLong("end_date");
        if (tmpDate > 0)
            EndDate = new Date(tmpDate * 1000);
        else
            EndDate = null;
        tmpDate = project.optLong("last_modified");
        if (tmpDate > 0)
            LastModified = new Date(tmpDate * 1000);
        else
            LastModified = null;
        NumberActiveTasks = project.optInt("nb_active_tasks");
        JSONObject urls = project.optJSONObject("url");
        if (urls != null) {
            ListURL = new URL(urls.optString("list"));
            BoardURL = new URL(urls.optString("board"));
            CalendarURL = new URL(urls.optString("calendar"));
        } else {
            ListURL = null;
            BoardURL = null;
            CalendarURL = null;
        }

        GroupedActiveTasks = new Hashtable<Integer, Dictionary<Integer, List<KanboardTask>>>();
        GroupedInactiveTasks = new Hashtable<>();
        GroupedOverdueTasks = new Hashtable<>();
        TaskHashtable = new Hashtable<>();
        CategoryHashtable = new Hashtable<>();

        Columns = new ArrayList<>();
        JSONArray cols = project.optJSONArray("columns");
        if (columns != null) {
            for (int i = 0; i < columns.length(); i++) {
                KanboardColumn tmpCol = new KanboardColumn(columns.optJSONObject(i));
                Columns.add(tmpCol);
                GroupedActiveTasks.put(tmpCol.getId(), new Hashtable<Integer, List<KanboardTask>>());
            }
        }
        else if (cols != null) {
            for (int i = 0; i < cols.length(); i++)
                Columns.add(new KanboardColumn(cols.optJSONObject(i)));
        }

        Swimlanes = new ArrayList<>();
        if (swimlanes != null) {
            for (int i = 0; i < swimlanes.length(); i++) {
                KanboardSwimlane tmpSwim = new KanboardSwimlane(swimlanes.optJSONObject(i));
                Swimlanes.add(tmpSwim);
                for (KanboardColumn c: Columns) {
                    GroupedActiveTasks.get(c.getId()).put(tmpSwim.getId(), new ArrayList<KanboardTask>());

                GroupedInactiveTasks.put(tmpSwim.getId(), new ArrayList<KanboardTask>());
                GroupedOverdueTasks.put(tmpSwim.getId(), new ArrayList<KanboardTask>());
                }
            }
        }

        Categories = new ArrayList<>();
        if (categories != null) {
            for (int i = 0; i < categories.length(); i++) {
                KanboardCategory tmpCategory = new KanboardCategory(categories.optJSONObject(i));
                Categories.add(tmpCategory);
                CategoryHashtable.put(tmpCategory.getId(), tmpCategory);
            }
        }

        ActiveTasks = new ArrayList<>();
        if (activetasks != null)
            for (int i = 0; i < activetasks.length(); i++) {
                KanboardTask tmpActiveTask = new KanboardTask(activetasks.optJSONObject(i));
                TaskHashtable.put(tmpActiveTask.getId(), tmpActiveTask);
                ActiveTasks.add(tmpActiveTask);
                GroupedActiveTasks.get(tmpActiveTask.getColumnId()).get(tmpActiveTask.getSwimlaneId()).add(tmpActiveTask);
            }

        InactiveTasks = new ArrayList<>();
        if (inactivetasks != null)
            for (int i = 0; i < inactivetasks.length(); i++) {
                KanboardTask tmpInactiveTask = new KanboardTask(inactivetasks.optJSONObject(i));
                TaskHashtable.put(tmpInactiveTask.getId(), tmpInactiveTask);
                InactiveTasks.add(tmpInactiveTask);
                GroupedInactiveTasks.get(tmpInactiveTask.getSwimlaneId()).add(tmpInactiveTask);
            }

        OverdueTasks = new ArrayList<>();
        if (overduetasks != null)
            for (int i = 0; i < overduetasks.length(); i++) {
                KanboardTask tmpOverdueTask = new KanboardTask(overduetasks.optJSONObject(i));
                OverdueTasks.add(TaskHashtable.get(tmpOverdueTask.getId()));
                GroupedOverdueTasks.get(TaskHashtable.get(tmpOverdueTask.getId()).getSwimlaneId()).add(TaskHashtable.get(tmpOverdueTask.getId()));
            }
    }

    public int getId() {
        return Id;
    }

    public String getName() {
        return Name;
    }

    public int getOwnerId() {
        return OwnerId;
    }

    public String getDescription() {
        return Description;
    }

    public String getIdentifier() {
        return Identifier;
    }

    public String getToken() {
        return Token;
    }

    public boolean getIsActive() {
        return IsActive;
    }

    public boolean getIsPublic() {
        return IsPublic;
    }

    public boolean getIsPrivate() {
        return IsPrivate;
    }

    public boolean getIsEverybodyAllowed() {
        return IsEverybodyAllowed;
    }

    public Date getStartDate() {
        return StartDate;
    }

    public Date getEndDate() {
        return EndDate;
    }

    public Date getLastModified() {
        return LastModified;
    }

    public int getNumberActiveTasks() {
        return NumberActiveTasks;
    }

    public URL getListURL() {
        return ListURL;
    }

    public URL getBoardURL() {
        return BoardURL;
    }

    public URL getCalendarURL() {
        return CalendarURL;
    }

    public List<KanboardColumn> getColumns() {
        return Columns;
    }

    public List<KanboardCategory> getCategories() {
        return Categories;
    }

    public List<KanboardSwimlane> getSwimlanes() {
        return Swimlanes;
    }

    public List<KanboardTask> getActiveTasks() {
        return ActiveTasks;
    }

    public Dictionary<Integer, Dictionary<Integer, List<KanboardTask>>> getGroupedActiveTasks() {
        return GroupedActiveTasks;
    }

    public List<KanboardTask> getInactiveTasks() {
        return InactiveTasks;
    }

    public Dictionary<Integer, List<KanboardTask>> getGroupedInactiveTasks() {
        return GroupedInactiveTasks;
    }

    public List<KanboardTask> getOverdueTasks() {
        return OverdueTasks;
    }

    public Dictionary<Integer, List<KanboardTask>> getGroupedOverdueTasks() {
        return GroupedOverdueTasks;
    }

    public Dictionary<Integer, KanboardTask> getTaskHashtable() {
        return TaskHashtable;
    }

    public Dictionary<Integer, KanboardCategory> getCategoryHashtable() {
        return CategoryHashtable;
    }

    @Override
    public int compareTo(@NonNull KanboardProject o) {
        return this.Name.compareTo(o.Name);
    }

    @Override
    public String toString() {
        return this.Name;
    }
}
