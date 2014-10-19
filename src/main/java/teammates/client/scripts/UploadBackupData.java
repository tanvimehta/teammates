package teammates.client.scripts;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Set;

import com.google.gson.Gson;

import teammates.client.remoteapi.RemoteApiClient;
import teammates.common.datatransfer.AccountAttributes;
import teammates.common.datatransfer.CommentAttributes;
import teammates.common.datatransfer.FeedbackQuestionAttributes;
import teammates.common.datatransfer.FeedbackResponseAttributes;
import teammates.common.datatransfer.FeedbackResponseCommentAttributes;
import teammates.common.datatransfer.FeedbackSessionAttributes;
import teammates.common.datatransfer.InstructorAttributes;
import teammates.common.datatransfer.CourseAttributes;
import teammates.common.datatransfer.DataBundle;
import teammates.common.datatransfer.EvaluationAttributes;
import teammates.common.datatransfer.StudentAttributes;
import teammates.common.datatransfer.SubmissionAttributes;
import teammates.common.exception.EntityAlreadyExistsException;
import teammates.common.exception.EntityDoesNotExistException;
import teammates.common.exception.InvalidParametersException;
import teammates.common.util.FileHelper;
import teammates.common.util.Utils;
import teammates.logic.api.Logic;

import teammates.logic.core.SubmissionsLogic;

import teammates.storage.api.CommentsDb;
import teammates.storage.api.CoursesDb;
import teammates.storage.api.FeedbackQuestionsDb;
import teammates.storage.api.FeedbackResponseCommentsDb;
import teammates.storage.api.FeedbackResponsesDb;
import teammates.storage.api.FeedbackSessionsDb;
import teammates.storage.api.InstructorsDb;
import teammates.storage.api.StudentsDb;
import teammates.storage.datastore.Datastore;


/**
 * Usage: This script imports a large data bundle to the appengine. The target of the script is the app with
 * appID in the test.properties file.Can use DataGenerator.java to generate random data.
 * 
 * Notes:
 * -Edit SOURCE_FILE_NAME before use
 * -Should not have any limit on the size of the databundle. However, the number of entities per request
 * should not be set to too large as it may cause Deadline Exception (especially for evaluations)
 * 
 */
public class UploadBackupData extends RemoteApiClient {

    private static String BACKUP_FOLDER = "Backup";
 
    
    private static DataBundle data;
    private static Gson gson = Utils.getTeammatesGson();
    private static String jsonString;
    
    private static Set<String> coursesPersisted;
    
    private static Logic logic = new Logic();
    private static SubmissionsLogic submissionsLogic = new SubmissionsLogic();
    private static final CoursesDb coursesDb = new CoursesDb();
    private static final CommentsDb commentsDb = new CommentsDb();
    private static final StudentsDb studentsDb = new StudentsDb();
    private static final InstructorsDb instructorsDb = new InstructorsDb();
    private static final FeedbackSessionsDb fbDb = new FeedbackSessionsDb();
    private static final FeedbackQuestionsDb fqDb = new FeedbackQuestionsDb();
    private static final FeedbackResponsesDb frDb = new FeedbackResponsesDb();
    private static final FeedbackResponseCommentsDb fcDb = new FeedbackResponseCommentsDb();
    
    public static void main(String args[]) throws Exception {
        UploadBackupData uploadBackupData = new UploadBackupData();
        uploadBackupData.doOperationRemotely();
    }
    
    protected void doOperation() {
        Datastore.initialize();
        File backupFolder = new File(BACKUP_FOLDER);
        String[] folders = backupFolder.list();
        for(String folder : folders) {
            String[] backupFiles = getBackupFilesInFolder(folder);
            uploadData(backupFiles, folder);
        }
    }
    private static String[] getBackupFilesInFolder(String folder) {
        String folderName = BACKUP_FOLDER + "/" + folder;
        File currentFolder = new File(folderName);   
        return currentFolder.list();
    }
    
    private static void uploadData(String[] backupFiles, String folder) {
        for(String backupFile : backupFiles) {
            try {
                String folderName = BACKUP_FOLDER + "/" + folder;
                jsonString = FileHelper.readFile(folderName + "/" + backupFile);
                data = gson.fromJson(jsonString, DataBundle.class);  
                if (!data.accounts.isEmpty()) {                  // Accounts
                    persistAccounts(data.accounts);
                }                      
                if (!data.courses.isEmpty()){                    // Courses
                    persistCourses(data.courses);
                } 
                if (!data.instructors.isEmpty()){                // Instructors
                    persistInstructors(data.instructors);
                } 
                if (!data.students.isEmpty()){                   // Students
                    persistStudents(data.students);
                } 
                if (!data.evaluations.isEmpty()){                // Evaluations
                    persistEvaluations(data.evaluations);
                } 
                if (!data.feedbackSessions.isEmpty()){           // Feedback sessions
                    persistFeedbackSessions(data.feedbackSessions);
                } 
                if (!data.feedbackQuestions.isEmpty()){          // Feedback questions
                    persistFeedbackQuestions(data.feedbackQuestions);
                } 
                if(!data.feedbackResponses.isEmpty()) {          // Feedback responses
                    persistFeedbackResponses(data.feedbackResponses);
                    
                } 
                if (!data.feedbackResponseComments.isEmpty()){   // Feedback response comments
                    persistFeedbackResponseComments(data.feedbackResponseComments);
                } 
                if (!data.submissions.isEmpty()){                // Submissions
                    persistSubmissions(data.submissions);;
                } 
                if(!data.comments.isEmpty()) {                   // Comments
                    persistComments(data.comments);
                }
            } catch (Exception e) {
                System.out.println("Error in uploading files: " + e.getMessage());
            }
        }
    }
    
    private static void persistAccounts(HashMap<String, AccountAttributes> accounts) {
        try {
            for(AccountAttributes accountData : accounts.values())
                logic.createAccount(accountData.googleId, accountData.name, 
                    accountData.isInstructor, accountData.email, accountData.institute);
        } catch (InvalidParametersException | EntityAlreadyExistsException | EntityDoesNotExistException e) {
            System.out.println("Error in uploading accounts: " + e.getMessage());
        }
    }
    
    private static void persistCourses(HashMap<String, CourseAttributes> courses) {
        try {
            coursesDb.createCourses(courses.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading courses: " + e.getMessage());
        }
    }
    
    private static void persistInstructors(HashMap<String, InstructorAttributes> instructors) {
        try {
            instructorsDb.createInstructors(instructors.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading instructors: " + e.getMessage());
        }
    }
    
    private static void persistStudents(HashMap<String, StudentAttributes> students) {
        try {
            studentsDb.createStudents(students.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading students: " + e.getMessage());
        }
    }
    
    private static void persistEvaluations(HashMap<String, EvaluationAttributes> evaluations) {

        for (EvaluationAttributes evaluation : evaluations.values()) {
            try {
                logic.createEvaluationWithoutSubmissionQueue(evaluation);
            } catch (EntityAlreadyExistsException | InvalidParametersException
                    | EntityDoesNotExistException e) {
                System.out.println("Error in uploading evaluations: " + e.getMessage());
            }
        }
    }
    
    private static void persistFeedbackSessions(HashMap<String, FeedbackSessionAttributes> feedbackSessions) {
        try {
            fbDb.createFeedbackSessions(feedbackSessions.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading feedback sessions: " + e.getMessage());
        }
    }
    
    private static void persistFeedbackQuestions(HashMap<String, FeedbackQuestionAttributes> map) {
        HashMap<String, FeedbackQuestionAttributes> questions = map;
        List<FeedbackQuestionAttributes> questionList = new ArrayList<FeedbackQuestionAttributes>(questions.values());
        Collections.sort(questionList);
        for(FeedbackQuestionAttributes question : questionList){
            question.removeIrrelevantVisibilityOptions();
        }

        try {
            fqDb.createFeedbackQuestions(questionList);
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading feedback questions: " + e.getMessage());
        }
    }
    
    private static void persistFeedbackResponses(HashMap<String, FeedbackResponseAttributes> map) {
        HashMap<String, FeedbackResponseAttributes> responses = map;
        List<FeedbackResponseAttributes> responseList = new ArrayList<FeedbackResponseAttributes>(responses.values());
        try {
            frDb.createFeedbackResponses(responseList);
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading feedback responses: " + e.getMessage());
        }
    }
    
    private static void persistFeedbackResponseComments(HashMap<String, FeedbackResponseCommentAttributes> map) {
        HashMap<String, FeedbackResponseCommentAttributes> responseComments = map;
     
        try {
            fcDb.createFeedbackResponseComments(responseComments.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading feedback response comments: " + e.getMessage());
        }
    }
    
    private static void persistComments(HashMap<String, CommentAttributes> map) {
        HashMap<String, CommentAttributes> comments = map;
        try {
            commentsDb.createComments(comments.values());
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading comments: " + e.getMessage());
        }
    }
    
    private static void persistSubmissions(HashMap<String, SubmissionAttributes> submissions) {
        List<SubmissionAttributes> listOfSubmissionsToAdd = new ArrayList<SubmissionAttributes>();
        for(SubmissionAttributes submission : submissions.values()) {
            listOfSubmissionsToAdd.add(submission);
        }
        
        try {
            submissionsLogic.createSubmissions(listOfSubmissionsToAdd);
        } catch (InvalidParametersException e) {
            System.out.println("Error in uploading submissions: " + e.getMessage());
        }
    }
}
