package mclachlan.brewday.db.v2.remote.gdrive;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.FileContent;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.DriveScopes;
import com.google.api.services.drive.model.File;
import com.google.api.services.drive.model.FileList;
import java.io.*;
import java.net.URLConnection;
import java.security.GeneralSecurityException;
import java.util.*;
import mclachlan.brewday.db.v2.sensitive.SensitiveStore;

/**
 *
 */
public class GoogleDriveBackend
{
	private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();

	/**
	 * Scopes required by this brewday. Modifying these scopes will invalidate
	 * existing stored credentials.
	 */
	private static final List<String> SCOPES = List.of(DriveScopes.DRIVE_FILE);

	private static final String APP_NAME = "google.drive.backend.app.name";
	private static final String REMOTE_DIR_ID = "google.drive.backend.remote.directory.id";
	private static final String TOKENS_DIR_PATH = "google.drive.backend.tokens.dir.path";
	private static final String CREDENTIALS_JSON = "google.drive.backend.credentials.json";

	/*-------------------------------------------------------------------------*/
	/**
	 * @param applicationName
	 * 	Application name as it will appear in Google APIs
	 * @param credentialsJson
	 * 	The contents of the "credentials.json" file downloaded from the Google
	 * 	API Console. This will be stored securely.
	 * @param remoteDirectoryName
	 * 	The directory on google drive where brewday files will be stored.
	 * @param tokensDirectoryPath
	 * 	The local directory at which to store Google API tokens.
	 * @return
	 * 	The remote dir ID to be used later
	 */
	public String enable(
		String applicationName,
		String credentialsJson,
		String remoteDirectoryName,
		String tokensDirectoryPath) throws Exception
	{
		Drive service = getDriveService(applicationName, credentialsJson, tokensDirectoryPath);
		return createRemoteDirectory(remoteDirectoryName, service);
	}

	/*-------------------------------------------------------------------------*/
	private Drive getDriveService(
		String applicationName,
		String credentialsJson,
		String tokensDirectoryPath) throws GeneralSecurityException, IOException
	{
		// Build a new authorized API client service.
		NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		Credential cred = getCredentials(httpTransport, credentialsJson, tokensDirectoryPath);
		return new Drive.Builder(httpTransport, JSON_FACTORY, cred)
			.setApplicationName(applicationName)
			.build();
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Creates an authorized Credential object.
	 *
	 * @param httpTransport The network HTTP Transport.
	 * @return An authorized Credential object.
	 * @throws IOException If the credentials.json file cannot be found.
	 */
	private static Credential getCredentials(
		NetHttpTransport httpTransport,
		String credentials,
		String tokensDirectoryPath) throws IOException
	{
		// Load client secrets.
		InputStream in = new StringBufferInputStream(credentials);

		GoogleClientSecrets clientSecrets =
			GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

		// Build flow and trigger user authorization request.
		GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
			httpTransport, JSON_FACTORY, clientSecrets, SCOPES)
			.setDataStoreFactory(new FileDataStoreFactory(new java.io.File(tokensDirectoryPath)))
			.setAccessType("offline")
			.build();
		LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8888).build();
		return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Thanks Google but we'd rather look up by name.
	 */
	private String getFileId(String fileName, Drive.Files files) throws IOException
	{
		List<File> fileList = files.
			list().
			setFields("files(id, name)").
			execute().
			getFiles();

		for (File f : fileList)
		{
			if (fileName.equals(f.getName()))
			{
				return f.getId();
			}
		}

		return null;
	}

	/*-------------------------------------------------------------------------*/
	private String createRemoteDirectory(
		String folderName,
		Drive driveService) throws IOException
	{
		File fileMetadata = new File();
		fileMetadata.setName(folderName);
		fileMetadata.setMimeType("application/vnd.google-apps.folder");

		File file = driveService.files().create(fileMetadata)
		    .setFields("id")
		    .execute();

		return file.getId();
	}

	/*-------------------------------------------------------------------------*/

	/**
	 * Creates or updates a file at the destination. Files with the same name
	 * are updated, regardless of Drive's non-uniqueness of names
	 */
	private void copyToRemote(
		java.io.File filePath,
		String fileMimeType,
		String folderId,
		Drive driveService) throws IOException
	{
		Drive.Files files = driveService.files();
		FileContent mediaContent = new FileContent(fileMimeType, filePath);

		String fileId = getFileId(filePath.getName(), files);

		if (fileId == null)
		{
			File fileMetadata = new File();
			fileMetadata.setMimeType(fileMimeType);
			fileMetadata.setName(filePath.getName());
			fileMetadata.setParents(Collections.singletonList(folderId));

			files.create(fileMetadata, mediaContent).execute();
		}
		else
		{
			File fileMetadata = new File();
			fileMetadata.setName(filePath.getName());
			fileMetadata.setDescription("Updated by Brewday sync");

			File file = files
				.update(fileId, fileMetadata, mediaContent)
				.execute();

			System.out.println("file = [" + file + "]");
		}
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Disables google drive integration. Google API credentials are deleted,
	 * no content on GDrive is changed.
	 */
	public void disable(
		SensitiveStore sensitiveStore,
		Map<String, String> settingsStore)
	{
		// todo
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Copies the current Brewday database to the given folder on Google drive.
	 * Remote contents will be overwritten.
	 * @param files
	 * 	the files to sync
	 * @param credentials
	 * @param remoteDirId
	 * @param appName
	 * @param tokensDirPath
	 */
	public void syncToRemote(
		List<java.io.File> files,
		String credentials,
		String remoteDirId,
		String appName,
		String tokensDirPath) throws Exception
	{
		Drive driveService = getDriveService(appName, credentials, tokensDirPath);

		for (java.io.File file : files)
		{
			copyToRemote(file, getFileMimeType(file), remoteDirId, driveService);
		}
	}

	/*-------------------------------------------------------------------------*/
	private String getFileMimeType(java.io.File file)
	{
		return URLConnection.guessContentTypeFromName(file.getName());
	}

	/*-------------------------------------------------------------------------*/
	/**
	 * Copies the remote Brewday database to local storage. Local contents will
	 * be overwritten.
	 */
	public void syncFromRemote(
		String localDirectory,
		String credentials,
		String appName,
		String tokensDirPath,
		String folderId) throws Exception
	{
		Drive driveService = getDriveService(appName, credentials, tokensDirPath);

		FileList fileList = driveService.files().list().
			setQ("'"+folderId+"' in parents and mimeType != 'application/vnd.google-apps.folder' and trashed = false").
			setFields("files(id, name, parents)")
			.execute();

		List<File> files = fileList.getFiles();

		for (File f : files)
		{
			OutputStream outputStream =
				new FileOutputStream(new java.io.File(localDirectory, f.getName()));

			driveService.files().get(f.getId())
				.executeMediaAndDownloadTo(outputStream);
		}
	}

	/*-------------------------------------------------------------------------*/
	public static void main(String[] args) throws Exception
	{
		GoogleDriveBackend test = new GoogleDriveBackend();

		SensitiveStore ss = new SensitiveStore("db/sensitive", "brewday");
		ss.init("342243bb-771c-4e71-b1b1-0e309a4864ce");

		String credentialsJson = ss.get("google.api.credentials");
		System.out.println("credentialsJson = [" + credentialsJson + "]");

		String appName = "Brewday";
		String tokensDirectoryPath = "./db/sensitive/tokens";

		String folderId = "1isNhil9ESxEyMgNqtZ69kRUyUURqCdDR";

		// Build a new authorized API client service.
		NetHttpTransport httpTransport = GoogleNetHttpTransport.newTrustedTransport();
		Credential cred = getCredentials(httpTransport, credentialsJson, tokensDirectoryPath);
		Drive driveService = new Drive.Builder(httpTransport, JSON_FACTORY, cred)
			.setApplicationName(appName)
			.build();

		java.io.File file = new java.io.File("./db/batches.json");

		String fileMimeType = "application/json";
		Drive.Files driveFiles = driveService.files();
		FileContent mediaContent = new FileContent(fileMimeType, file);

//		String fileId = "1tnxSCQkQdvgulmVe5GeRchT-jm57F1pH";
		String fileId = "1tnxSCQkQdvgulmVe5GeRchT-jm57F1pH";

		File fileMetadata = new File();
		fileMetadata.setName(file.getName());
		fileMetadata.setMimeType(fileMimeType);
		fileMetadata.setDescription("Updated by sync");


		File f = driveFiles
			.update(fileId, fileMetadata, mediaContent)
			.execute();
		System.out.println("f = [" + f + "]");

		// OTOH, this works fine
//		fileMetadata.setParents(Collections.singletonList(folderId));
//		driveFiles.create(fileMetadata, mediaContent).execute();
	}
}
