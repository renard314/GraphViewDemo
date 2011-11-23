package de.inovex.graph.demo.contentprovider;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.text.TextUtils;
import android.util.Log;

public class RWELiveDataContentProvider extends ContentProvider {

	private final static String TAG = RWELiveDataContentProvider.class.toString();
	private static final String AUTHORITY = "de.inovex.graph.demo";

	public static final Uri CONTENT_URI = Uri.parse("content://" + AUTHORITY);
	public static final Uri CONTENT_URI_PLACES = Uri.parse("content://" + AUTHORITY + "/locations");
	public static final Uri CONTENT_URI_PLACES_COUNT = Uri.parse("content://" + AUTHORITY + "/locations/count");
	public static final Uri CONTENT_URI_PRODUCTION = Uri.parse("content://" + AUTHORITY + "/production_data");
	public static final Uri CONTENT_URI_PRODUCTION_TOTAL = Uri.parse("content://" + AUTHORITY + "/production_data/total/minute");
	public static final Uri CONTENT_URI_PRODUCTION_TOTAL_WIND = Uri.parse("content://" + AUTHORITY + "/production_data/total/wind");
	public static final Uri CONTENT_URI_PRODUCTION_TOTAL_BIO = Uri.parse("content://" + AUTHORITY + "/production_data/total/bio");
	public static final Uri CONTENT_URI_PRODUCTION_TOTAL_WATER = Uri.parse("content://" + AUTHORITY + "/production_data/total/water");

	public static enum POWER_TYPE {
		BIOMASS("Biomassekraftwerk"), ONSHORE_WIND("Onshore Windpark"), WATER("Wasserkraftwerk"), CHP_COAL("CHP Kohlekraftwerk"), UNKNOWN("");
		private final String id;

		private POWER_TYPE(String id) {
			this.id = id;

		}

		public String getName() {
			return id;
		}

		public static final POWER_TYPE fromString(String name) {
			for (POWER_TYPE type : POWER_TYPE.values()) {
				if (name.contains(type.id)) {
					return type;
				}
			}
			return UNKNOWN;
		}
	}

	public static class Columns {
		public static class Locations {
			public static final String ID = "_id";
			public static final String CREATED = "created";
			public static final String NAME = "name";
			public static final String TYPE = "type";
			public static final String CITY = "city";
			public static final String COUNTRY = "country";
			public static final String GOLIVE = "golive";
			public static final String TURBINES = "turbines";
			public static final String POWER = "power";
			public static final String LOCATION_ID = "location_id";
			public static final String XPOS = "xpos";
			public static final String YPOS = "ypos";
			public static final String LAST_PRODUCTION = "last_production";
		}

		public static class ProductionData {
			public static final String ID = "_id";
			public static final String CREATED = "created";
			public static final String LOCATION_ID = "location_id";
			public static final String VALUE = "value";
			public static final String TOTAL = "total"; // virtual column
		}
	}

	private static final UriMatcher sUriMatcher;
	private static final int PRODUCTION_DATA = 0;
	private static final int LOCATION = 1;
	private static final int LOCATIONS = 2;
	private static final int PRODUCTION_DATA_TOTAL = 3;
	private static final int LOCATIONS_COUNT = 4;
	private static final int PRODUCTION_DATA_TOTAL_WIND = 5;
	private static final int PRODUCTION_DATA_TOTAL_WATER = 6;
	private static final int PRODUCTION_DATA_TOTAL_BIO = 7;

	static {
		sUriMatcher = new UriMatcher(UriMatcher.NO_MATCH);
		sUriMatcher.addURI(AUTHORITY, "locations/#", LOCATION);
		sUriMatcher.addURI(AUTHORITY, "locations", LOCATIONS);
		sUriMatcher.addURI(AUTHORITY, "locations/count", LOCATIONS_COUNT);
		sUriMatcher.addURI(AUTHORITY, "production_data", PRODUCTION_DATA);
		sUriMatcher.addURI(AUTHORITY, "production_data/total/minute", PRODUCTION_DATA_TOTAL);
		sUriMatcher.addURI(AUTHORITY, "production_data/total/wind", PRODUCTION_DATA_TOTAL_WIND);
		sUriMatcher.addURI(AUTHORITY, "production_data/total/water", PRODUCTION_DATA_TOTAL_WATER);
		sUriMatcher.addURI(AUTHORITY, "production_data/total/bio", PRODUCTION_DATA_TOTAL_BIO);
	}

	private static class DBHelper extends SQLiteOpenHelper {

		private static final String LOCATION_TABLE_NAME = "locations";
		private static final String PRODUCTION_DATA_TABLE_NAME = "production_data";
		private static final int DATABASE_VERSION = 99;

//		private static final String LOCATION_TABLE_CREATE = "CREATE TABLE " + LOCATION_TABLE_NAME + " ( " + Columns.Locations.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//				+ Columns.Locations.LOCATION_ID + " TEXT UNIQUE ON CONFLICT REPLACE, " + Columns.Locations.CREATED + " INTEGER, " + Columns.Locations.NAME + " TEXT, " + Columns.Locations.TYPE
//				+ " TEXT, " + Columns.Locations.CITY + " TEXT, " + Columns.Locations.COUNTRY + " TEXT, " + Columns.Locations.GOLIVE + " TEXT, " + Columns.Locations.XPOS + " INTEGER, "
//				+ Columns.Locations.YPOS + " INTEGER, " + Columns.Locations.LAST_PRODUCTION + " INTEGER, " + Columns.Locations.TURBINES + " INTEGER, " + Columns.Locations.POWER + " TEXT );";
//
//		private static final String PRODUCTION_DATA_TABLE_CREATE = "CREATE TABLE " + PRODUCTION_DATA_TABLE_NAME + " ( " + Columns.ProductionData.ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
//				+ Columns.ProductionData.CREATED + " INTEGER, " + Columns.ProductionData.VALUE + " INTEGER, " + Columns.ProductionData.LOCATION_ID + " TEXT);";

		private static final String DATABASE_NAME = "inovex_graph_demo";
		private Context mContext;
		DBHelper(Context context) {
			super(context, DATABASE_NAME, null, DATABASE_VERSION);
			mContext = context;
		}
		
		 private boolean checkDataBase(){
			 
		    	SQLiteDatabase checkDB = null;
		 
		    	try{
		    		String myPath =  "/data/data/de.inovex.graph.demo/databases/inovex_graph_demo";
		    		checkDB = SQLiteDatabase.openDatabase(myPath, null, SQLiteDatabase.OPEN_READONLY);
		 
		    	}catch(SQLiteException e){
		 
		    		//database does't exist yet.
		 
		    	}
		 
		    	if(checkDB != null){
		 
		    		checkDB.close();
		 
		    	}
		 
		    	return checkDB != null ? true : false;
		    }

		@Override
		public void onCreate(SQLiteDatabase db) {
		}

		/**
		 * 92 * Copies your database from your local assets-folder to the just
		 * created 93 * empty database in the system folder, from where it can
		 * be accessed and 94 * handled. This is done by transfering bytestream.
		 * 95 *
		 */
		private void copyDataBase() {
			try {
				AssetManager assets = mContext.getAssets();
				InputStream myInput = assets.open(DATABASE_NAME);
				// Path to the just created empty db
				String outFileName = "/data/data/de.inovex.graph.demo/databases/inovex_graph_demo";
				// Open the empty db as the output stream
				OutputStream myOutput = new FileOutputStream(outFileName);

				// transfer bytes from the inputfile to the outputfile
				byte[] buffer = new byte[1024];
				int length;
				while ((length = myInput.read(buffer)) > 0) {
					myOutput.write(buffer, 0, length);
				}
				// Close the streams
				myOutput.flush();
				myOutput.close();
				myInput.close();
			} catch (IOException exc) {
				Log.e(RWELiveDataContentProvider.class.getName(),"import error");
				exc.printStackTrace();
			}

		}

		@Override
		public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

			//Log.w(TAG, "Upgrading database from version " + oldVersion +" to " + newVersion + ", which will destroy all old data");
			//onCreate(db);
		}

	}

	private DBHelper dbHelper;
	SQLiteStatement mLocationCounter;

	@Override
	public boolean onCreate() {
		dbHelper = new DBHelper(getContext());
		if (!dbHelper.checkDataBase()){
			dbHelper.getReadableDatabase();
			dbHelper.copyDataBase();
			SQLiteDatabase backup = dbHelper.getWritableDatabase();
			backup.close();
			backup = dbHelper.getWritableDatabase();
			backup.execSQL("DELETE FROM production_data WHERE created<1321388353770 OR created>1321441881270;");
			//backup.execSQL("DELETE FROM production_data WHERE ROWID%2=0");
			backup.close();

		}
		mLocationCounter = dbHelper.getReadableDatabase().compileStatement("select count(*) from locations;");
		return true;
	}

	@Override
	public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs, String sortOrder) {
		SQLiteQueryBuilder qb = new SQLiteQueryBuilder();
		String limit = null;
		int type = sUriMatcher.match(uri);

		String groupBy = null;

		switch (type) {
		case PRODUCTION_DATA_TOTAL_WATER:
			// SELECT p.created,sum(p.value), l.type FROM production_data AS p
			// LEFT OUTER JOIN locations AS l ON (p.location_id = l.location_id)
			// WHERE l.type LIKE "%Bio%" GROUP BY p.created;
			qb.setTables("production_data AS p LEFT OUTER JOIN locations AS l ON (p.location_id= l.location_id)");
			projection = new String[] { "p." + RWELiveDataContentProvider.Columns.ProductionData.CREATED + " AS created",
					"SUM(p." + RWELiveDataContentProvider.Columns.ProductionData.VALUE + ") AS " + Columns.ProductionData.TOTAL };
			groupBy = "p." + RWELiveDataContentProvider.Columns.ProductionData.CREATED;
			if (selection!=null){

				selection += " AND l.type LIKE \"%Wasser%\"";
			} else {
				selection = "l.type LIKE \"%Wasser%\"";				
			}
			sortOrder = "p.created ASC";
			break;
		case PRODUCTION_DATA_TOTAL_BIO:
			// SELECT p.created,sum(p.value), l.type FROM production_data AS p
			// LEFT OUTER JOIN locations AS l ON (p.location_id = l.location_id)
			// WHERE l.type LIKE "%Bio%" GROUP BY p.created;
			qb.setTables("production_data AS p LEFT OUTER JOIN locations AS l ON (p.location_id= l.location_id)");
			projection = new String[] { "p." + RWELiveDataContentProvider.Columns.ProductionData.CREATED + " AS created",
					"SUM(p." + RWELiveDataContentProvider.Columns.ProductionData.VALUE + ") AS " + Columns.ProductionData.TOTAL };
			groupBy = "p." + RWELiveDataContentProvider.Columns.ProductionData.CREATED;
			if (selection!=null){
				selection += " AND l.type LIKE \"%Bio%\"";
			} else {
				selection = "l.type LIKE \"%Bio%\"";				
			}
			sortOrder = "p.created ASC";
			break;
		case PRODUCTION_DATA_TOTAL_WIND:
			// SELECT p.created,sum(p.value), l.type FROM production_data AS p
			// LEFT OUTER JOIN locations AS l ON (p.location_id = l.location_id)
			// WHERE l.type LIKE "%Bio%" GROUP BY p.created;
			qb.setTables("production_data AS p LEFT OUTER JOIN locations AS l ON (p.location_id= l.location_id)");
			projection = new String[] { "p." + RWELiveDataContentProvider.Columns.ProductionData.CREATED + " AS created",
					"SUM(p." + RWELiveDataContentProvider.Columns.ProductionData.VALUE + ") AS " + Columns.ProductionData.TOTAL };
			groupBy = "p." + RWELiveDataContentProvider.Columns.ProductionData.CREATED;
			if (selection!=null){
				selection += " AND l.type LIKE \"%Wind%\"";				
			} else {
				selection = "l.type LIKE \"%Wind%\"";
			}
			sortOrder = "p.created ASC";
			break;
		case PRODUCTION_DATA_TOTAL:
			qb.setTables(DBHelper.PRODUCTION_DATA_TABLE_NAME);
			projection = new String[] { RWELiveDataContentProvider.Columns.ProductionData.CREATED,
					"SUM(" + RWELiveDataContentProvider.Columns.ProductionData.VALUE + ") AS " + Columns.ProductionData.TOTAL };
			groupBy = RWELiveDataContentProvider.Columns.ProductionData.CREATED;
			break;
		case PRODUCTION_DATA:
			qb.setTables(DBHelper.PRODUCTION_DATA_TABLE_NAME);
			groupBy = Columns.ProductionData.ID;
			break;
		case LOCATION:
		case LOCATIONS:
			qb.setTables(DBHelper.LOCATION_TABLE_NAME);
			groupBy = Columns.Locations.ID;
			break;
		case LOCATIONS_COUNT:
			return dbHelper.getReadableDatabase().rawQuery("select count(*) from locations", null);
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		// If no sort order is specified use the default
		String orderBy;
		if (TextUtils.isEmpty(sortOrder)) {
			orderBy = "created DESC";
		} else {
			orderBy = sortOrder;
		}

		// Get the database and run the query
		SQLiteDatabase db = dbHelper.getReadableDatabase();
		Cursor c = qb.query(db, projection, selection, selectionArgs, groupBy, null, orderBy, limit);

		// Tell the cursor what uri to watch, so it knows when its source data
		// changes
		c.setNotificationUri(getContext().getContentResolver(), uri);
		return c;
	}

	@Override
	public String getType(Uri uri) {
		switch (sUriMatcher.match(uri)) {
		case PRODUCTION_DATA:
			return "vnd.android.cursor.dir/vnd.inovex.graph.data.production";
		case LOCATION:
			return "vnd.android.cursor.item/vnd.inovex.graph.data.location";
		case LOCATIONS:
			return "vnd.android.cursor.item/vnd.inovex.graph.data.locations";
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}
	}

	@Override
	public Uri insert(Uri uri, ContentValues values) {
		String tableName = null;
		String columnCreated = null;
		Uri notifyUri = null;

		if (values != null) {
			values = new ContentValues(values);
		} else {
			values = new ContentValues();
		}

		final int type = sUriMatcher.match(uri);
		switch (type) {
		case PRODUCTION_DATA:
			tableName = DBHelper.PRODUCTION_DATA_TABLE_NAME;
			columnCreated = Columns.ProductionData.CREATED;
			break;
		case LOCATION:
		case LOCATIONS:
			tableName = DBHelper.LOCATION_TABLE_NAME;
			columnCreated = Columns.Locations.CREATED;
			notifyUri = CONTENT_URI_PLACES;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		if (!values.containsKey(columnCreated)) {
			Long now = Long.valueOf(System.currentTimeMillis());
			values.put(columnCreated, now);
		}

		SQLiteDatabase db = dbHelper.getWritableDatabase();
		long rowId = db.insert(tableName, null, values);
		if (rowId > 0) {
			Uri entryUri = ContentUris.withAppendedId(uri, rowId);
			if (notifyUri != null) {
				getContext().getContentResolver().notifyChange(entryUri, null);
				getContext().getContentResolver().notifyChange(notifyUri, null);
			}
			return entryUri;
		} else {
			throw new SQLException("Failed to insert row into " + uri);
		}
	}

	@Override
	public int delete(Uri uri, String selection, String[] selectionArgs) {
		String tableName = null;

		SQLiteDatabase db = dbHelper.getWritableDatabase();

		int count = 0;
		final int type = sUriMatcher.match(uri);
		switch (type) {
		case PRODUCTION_DATA:
			tableName = DBHelper.PRODUCTION_DATA_TABLE_NAME;
			break;
		case LOCATION:
		case LOCATIONS:
			tableName = DBHelper.LOCATION_TABLE_NAME;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		count = db.delete(tableName, selection, selectionArgs);

		getContext().getContentResolver().notifyChange(uri, null);
		getContext().getContentResolver().notifyChange(CONTENT_URI, null);
		return count;
	}

	@Override
	public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
		String tableName = null;

		SQLiteDatabase db = dbHelper.getWritableDatabase();

		int count = 0;
		final int type = sUriMatcher.match(uri);
		switch (type) {
		case PRODUCTION_DATA:
			tableName = DBHelper.PRODUCTION_DATA_TABLE_NAME;
			break;
		case LOCATION:
		case LOCATIONS:
			tableName = DBHelper.LOCATION_TABLE_NAME;
			break;
		default:
			throw new IllegalArgumentException("Unknown URI " + uri);
		}

		count = db.update(tableName, values, selection, selectionArgs);
		getContext().getContentResolver().notifyChange(uri, null);
		return count;
	}

}
