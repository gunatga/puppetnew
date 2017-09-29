package in.vamsoft.sphinx.exammaster.service;

import in.vamsoft.sphinx.exammaster.entity.ExamSetUp;
import in.vamsoft.sphinx.exammaster.entity.ExamTopicDetail;
import in.vamsoft.sphinx.exammaster.entity.Questions;
import in.vamsoft.sphinx.exammaster.entity.UploadMaster;
import in.vamsoft.sphinx.exammaster.repository.ExamMasterRepository;
import in.vamsoft.sphinx.exammaster.repository.ExamTopicDetailRepository;
import in.vamsoft.sphinx.exammaster.repository.QuestionsRepository;
import in.vamsoft.sphinx.usermaster.entity.UserAdminRelationship;
import in.vamsoft.sphinx.usermaster.entity.UserExamRelationship;
import in.vamsoft.sphinx.usermaster.entity.UserMasterBean;
import in.vamsoft.sphinx.usermaster.entity.UserMasterTable;
import in.vamsoft.sphinx.usermaster.repository.DetailedUserPerformanceRepository;
import in.vamsoft.sphinx.usermaster.repository.UserAdminRelationshipRepository;
import in.vamsoft.sphinx.usermaster.repository.UserExamRelationshipRepository;
import in.vamsoft.sphinx.usermaster.repository.UserMasterRepository;
import in.vamsoft.sphinx.usermaster.repository.UserMasterTableRepository;
import in.vamsoft.sphinx.usermaster.repository.UserPerformanceRepository;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.servlet.http.HttpSession;

import jxl.Cell;
import jxl.Sheet;
import jxl.Workbook;
import jxl.read.biff.BiffException;

import org.apache.log4j.Logger;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Transactional
@Service
public class UploadExcelServiceImpl implements UploadExcelService {

	private static final Logger LOGGER = Logger
			.getLogger(UploadExcelServiceImpl.class);

	@Autowired
	private UserMasterRepository userMasterRepository;
	@Autowired
	private ExamMasterRepository examMasterRepository;

	@Autowired
	private DetailedUserPerformanceRepository detailedUserPerformanceRepository;

	@Autowired
	private UserPerformanceRepository userPerformanceRepository;

	@Autowired
	private UserExamRelationshipRepository userExamRelationshipRepository;

	@Autowired
	private UserAdminRelationshipRepository userAdminRelationshipRepository;

	@Autowired
	private ExamTopicDetailRepository topicDetailRepository;

	@Autowired
	private QuestionsRepository questionsRepository;

	@Autowired
	private UserMasterTableRepository userMasterTableRepository;

	@Autowired
	private ExamSetupMasterBeanservices masterBeanservice;
	// check file already available or not
	@Override
	public boolean fileExists(String fileName, String path)
			throws FileNotFoundException, IOException, BiffException {
		File uploadedFile = null;

		uploadedFile = new File(path);
		boolean exists = uploadedFile.exists();
		LOGGER.info(exists + "exists");
		if (exists == true) {
			return true;
		}
		return false;
	}

	// to find file format is xls
	@Override
	public boolean findFileType(String path, String fileName) {
		char extensionSeparator = '.';
		int dot = path.lastIndexOf(extensionSeparator);
		String extension = path.substring(dot + 1);

		LOGGER.info("extension" + extension);
		if (extension.equals("xls")) {

			LOGGER.info(extension);
			if (extension.equals("xls")) {

				return true;
			}
			return false;
		}
		return false;
	}

	// upload users
	@Override
	public boolean uploadUsers(String fileName, String path, String examId,
			String examName, String adminId, HttpSession httpSession,
			UploadMaster uploadMaster, UserMasterBean userMasterBean,
			UserExamRelationship examRelationship,
			UserAdminRelationship userAdminRelationship) throws BiffException,
			IOException {
		LOGGER.info("UploadExcelServiceImpl.uploadUsers()..................");

		File uploadedFile = new File(path);

		Workbook workbook = Workbook.getWorkbook(uploadedFile);
		Sheet sheet = workbook.getSheet(0);
		int rows = sheet.getRows();
		LOGGER.info("user rows ..." + rows);
		int cols = sheet.getColumns();

		LOGGER.info("column" + cols);

		if (cols < 6) {
			LOGGER.info("fileNotInProperFormat"
							+ "The Excel file is not in proper format. It must have minimum  6 columns. Please see the help document for the proper file structure.");

			return (Boolean) null;
		}

		int failedInserts = 0;
		List failedUsersList = new ArrayList();
		List assignedUsersList = new ArrayList();
		List userNotBelongsToThisAdminList = new ArrayList();

		/*
		 * Set the id value used another process..(in list user upload in
		 * current session)
		 */
		ArrayList uploadedUsersList = (ArrayList) httpSession
				.getAttribute("uploadedUsersList");

		LOGGER.info("uploadedUsersList" + uploadedUsersList);
		if (uploadedUsersList == null) {
			uploadedUsersList = new ArrayList();

			LOGGER.info("uploaduser" + uploadedUsersList);
			httpSession.setAttribute("uploadedUsersList", uploadedUsersList);
			LOGGER.info("uploaduser" + uploadedUsersList);

		}

		for (int i = 1; i < rows; i++) {

			Cell[] cellarr = sheet.getRow(i);
			LOGGER.info("cell  length is " + cellarr.length);
			if (cellarr.length > 0) {
				String id = cellarr[1].getContents().trim(); // List User upload
																// in the
																// Current
																// Session using
																// this id
				LOGGER.info("people id is " + id);
				if ((id != null) && (id.length() > 1)) {
					LOGGER.info("inside upload user if condition" + id.length());
					uploadedUsersList.add(id); // adding the userid in Array
												// list
					userMasterBean.setUserid(id);
					LOGGER.info("id.1...." + id);
					userMasterBean
							.setFirstName(cellarr[2].getContents().trim());
					LOGGER.info("2......");
					userMasterBean.setLastName(cellarr[3].getContents().trim());
					LOGGER.info("3......");
					if (cellarr.length >= 4) {
						userMasterBean.setMailID(cellarr[4].getContents()
								.trim());
						LOGGER.info("4......");
					}
					if (cellarr.length > 5) {
						userMasterBean.setDept(cellarr[5].getContents().trim());
						LOGGER.info("5......");
					} else {
						userMasterBean.setDept("");
					}
					if (cellarr.length > 8) {
						LOGGER.info("8......");
						String enable = cellarr[8].getContents().trim();

						LOGGER.info("enable is " + enable);
						if ("0".equals(enable)) {
							// logger.debug("setting 0");
							userMasterBean.setEnable(false);
						} else {
							// logger.debug("setting 0");
							userMasterBean.setEnable(true);
						}
						if (cellarr.length > 9) {
							LOGGER.info("7......");
							String password = cellarr[9].getContents().trim();
							if ((password != null) && (password.length() > 1)) {
								userMasterBean.setPassword(password);
								LOGGER.info("^^^@@@@@^^After !!!!^^^^^^^^^^^^^^      "
										+ cellarr[9].getContents().trim());

							}
						}
						if (cellarr.length > 10) {
							LOGGER.info("8......");
							String changePassword = cellarr[10].getContents()
									.trim();
							LOGGER.info(changePassword);
							if ((changePassword != null)
									&& ("0".equals(changePassword))) {
								LOGGER.info("inside conditions..."
										+ changePassword);
								// logger.debug("change password is " +
								// changePassword);
								userMasterBean
										.setPasswordChangesAfterEachAttempt(false);
							}
						}
						if (cellarr.length > 11) {
							LOGGER.info("9......");
							String canSplitExamAttempts = cellarr[11]
									.getContents().trim();
							if ((canSplitExamAttempts != null)
									&& ("0".equals(canSplitExamAttempts))) {
								// logger.debug("change split exams is " +
								// canSplitExamAttempts);
								userMasterBean.setCanSplitExamAttempts(false);
							}
						}
						if (cellarr.length > 12) {
							LOGGER.info("10......");
							String canSeeDetailedAnswers = cellarr[12]
									.getContents().trim();
							if ((canSeeDetailedAnswers != null)
									&& ("0".equals(canSeeDetailedAnswers))) {
								// logger.debug("change see detailed answers  is "
								// + canSeeDetailedAnswers);
								userMasterBean.setCanSeeDetailedAnswers(false);
							}
						}
						if (cellarr.length > 13) {
							LOGGER.info("11......");
							String maxSplitAttemptsObj = cellarr[13]
									.getContents().trim();
							if (maxSplitAttemptsObj != null) {
								LOGGER.info("maximum split attempts  is "
										+ maxSplitAttemptsObj);

								try {
									LOGGER.info("maxSplitAttemptsObj"
											+ maxSplitAttemptsObj);
									int maxSplitAttempts = Integer
											.parseInt(maxSplitAttemptsObj);
									LOGGER.info("maxSplitAttempts"
											+ maxSplitAttempts);

									userMasterBean
											.setMaxSplitAttempts(maxSplitAttempts);
								} catch (NumberFormatException nfe) {
									LOGGER.info("cannot convert max split attempts ");

								}
							}

						}
						if (cellarr.length > 14) {
							LOGGER.info("12......");
							userMasterBean.setUserFunction(cellarr[14]
									.getContents().trim());
							LOGGER.info("cellarr[14].getContents().trim()   "
									+ cellarr[14].getContents().trim());
							// logger.debug("cellarr[14].getContents().trim()   "
							// + cellarr[14].getContents().trim());
						}
						if (cellarr.length > 15) {
							LOGGER.info("13......");
							String canPrintCertificateObj = cellarr[15]
									.getContents().trim();
							LOGGER.info("can print certificate   is "
									+ canPrintCertificateObj);
							// logger.debug("can print certificate   is " +
							// canPrintCertificateObj);
							try {
								int canPrintCertificate = Integer
										.parseInt(canPrintCertificateObj);
								userMasterBean
										.setCanPrintCertificate(canPrintCertificate);

							} catch (NumberFormatException nfe) {
								LOGGER.info("cannot convert print certificate ");
								// logger.debug("cannot convert print certificate ");
							}
						}
						LOGGER.info("userMasterBean///////" + userMasterBean);

					}
					// ------

					String userId = id;
					LOGGER.info("userid....." + userId);
					UserMasterTable moreuser = userMasterTableRepository
							.findByUserId(userId);
					LOGGER.info("moreuser............." + moreuser);

					if (moreuser != null) {
						failedInserts++;

						LOGGER.info("user is already there in the user master######");

						failedUsersList.add(id);
						updateUserHelper(userMasterBean);
					} else {
						addUserHelper(userMasterBean, userAdminRelationship,
								adminId);
					}
					LOGGER.info("userId..." + userId + "examId" + examId);
					BigInteger isUserAlreadyAssigned = userExamRelationshipRepository
							.selectOne(userId, examId);

					LOGGER.info("user has been assigned "
							+ isUserAlreadyAssigned);

					boolean userAlreadyAssigned = false;

					if (isUserAlreadyAssigned != null) {
						failedInserts++;

						LOGGER.info("user is already assigned to this exam ###########");

						assignedUsersList.add(id);
						LOGGER.info("list of user..new...uploadedUsersList......"
								+ assignedUsersList);

						userAlreadyAssigned = true;

						if (cellarr.length > 6) {
							String attempts = cellarr[6].getContents().trim();
							LOGGER.info("aA is " + attempts);
							try {
								uploadMaster.setAllowedAttempts(Integer
										.parseInt(attempts));
							} catch (NumberFormatException nfe) {
								LOGGER.info("cannot convert allowed attempts ");
							}
						}
						if (cellarr.length > 7) {
							String timeOutDays = cellarr[7].getContents()
									.trim();
							try {
								uploadMaster.setTimeoutDays(Integer
										.parseInt(timeOutDays));
							} catch (NumberFormatException nfe) {
								LOGGER.info("cannot convert time out days ");
							}
						}

						SimpleDateFormat sdf = new SimpleDateFormat(
								"yyyy-MM-dd");
						String curDate = sdf.format(new Date());

						LOGGER.info("can seee"
								+ userMasterBean.isCanSeeDetailedAnswers());
						Integer updateUserAssignStmt = userExamRelationshipRepository
								.AssignUser(
										userMasterBean.getUserid(),
										userMasterBean.getExamid(),
										uploadMaster.getAllowedAttempts(),
										uploadMaster.getTimeoutDays(),
										curDate,
										userMasterBean
												.isPasswordChangesAfterEachAttempt(),
										userMasterBean.isCanSplitExamAttempts(),
										userMasterBean
												.isCanSeeDetailedAnswers(),
										userMasterBean.getMaxSplitAttempts(),
										userMasterBean.getCanPrintCertificate());
						LOGGER.info("updateUserAssignStmt....S"
								+ updateUserAssignStmt);

					} else {						
						LOGGER.info("userId..." + userId + "adminId....."
								+ adminId);
						BigInteger checkAdminForThisUserStmt = userAdminRelationshipRepository
								.selectOneAdminid(userId, adminId);
						// String checkAdminForThisUserStmt="1";
						LOGGER.info("user belonging to this admin "
								+ checkAdminForThisUserStmt);

						if (checkAdminForThisUserStmt != null) {
							String userId1 = id;
							LOGGER.info("userId1.." + userId1);
							String examId1 = uploadMaster.getExamId();
							LOGGER.info("examId1.." + examId1);
							if (cellarr.length > 6) {
								String aA = cellarr[6].getContents();
								LOGGER.info("aA is " + aA);
								try {
									uploadMaster.setAllowedAttempts(Integer
											.parseInt(aA));
								} catch (NumberFormatException nfe) {
									LOGGER.info("cannot convert allowed attempts"
											+ aA);
								}
							}
							if (cellarr.length > 7) {

								String tod = cellarr[7].getContents();
								try {
									uploadMaster.setTimeoutDays(Integer
											.parseInt(tod));
								} catch (NumberFormatException nfe) {
									LOGGER.info("cannot convert time out days "
											+ tod);
								}

							}

							String allowdAttempts = String.valueOf(uploadMaster
									.getAllowedAttempts());
							LOGGER.info("allowdAttempts//" + allowdAttempts);
							String getTimeoutDays = String.valueOf(uploadMaster
									.getTimeoutDays());
							LOGGER.info("getTimeoutDays..." + getTimeoutDays);

							SimpleDateFormat sdf = new SimpleDateFormat(
									"yyyy-MM-dd");
							String curDate = sdf.format(new Date());
							String lastPerformanceDate = curDate;
							String passwordChangesAuto = String
									.valueOf(userMasterBean
											.isPasswordChangesAfterEachAttempt() ? 1
											: 0);
							String canSeeDetailedResults = String
									.valueOf(userMasterBean
											.isCanSeeDetailedAnswers() ? 1 : 0);
							String canSplitExams = String
									.valueOf(userMasterBean
											.isCanSplitExamAttempts() ? 1 : 0);
							String maxSplitAttempts = String
									.valueOf(userMasterBean
											.getMaxSplitAttempts());
							String certificatePrinted = String
									.valueOf(userMasterBean
											.getCanPrintCertificate());
							LOGGER.info(lastPerformanceDate
									+ "passwordChangesAuto.."
									+ passwordChangesAuto
									+ "canSeeDetailedResults.."
									+ canSeeDetailedResults
									+ "..canSplitExams==" + canSplitExams
									+ "maxSplitAttempts.." + maxSplitAttempts
									+ "certificatePrinted.."
									+ certificatePrinted);

							Integer result = userExamRelationshipRepository
									.thisUserBelongsToThisAdmin(userId1,
											examId1, allowdAttempts,
											getTimeoutDays,
											lastPerformanceDate,
											passwordChangesAuto,
											canSeeDetailedResults,
											canSplitExams, maxSplitAttempts,
											certificatePrinted);

							LOGGER.info("thisUserBelongsToThisAdmin ... insert success..."
									+ result);
						} else {
							userNotBelongsToThisAdminList.add(id);
						}
					}
				}
			}
			/*
			 * List user upload in session
			 */

			httpSession.setAttribute("uploadedUsersList", uploadedUsersList);
		}
		if (failedInserts > 0) {
			String message = "These Users were already available"
					+ failedUsersList;
			LOGGER.info("failed assigned user list size is "
					+ assignedUsersList.size());
			LOGGER.info("users belonging to anothe admin  list size is "
					+ userNotBelongsToThisAdminList.size());

			if (assignedUsersList.size() > 0) {
				message = message
						+ "these users have already been assigned to this exam"
						+ assignedUsersList;
			}
			if (userNotBelongsToThisAdminList.size() > 0) {
				message = message
						+ "These users are managed by a different admin "
						+ userNotBelongsToThisAdminList;
			}
			// JSFMessagePopulator.populateJSFMessage("partialSuccess",
			// message);

		}

		return true;

	}

	// upload questions
	@Override
	public String uploadQuestions(String fileName, String path, String examId,
			String examName, String adminId, Questions questionMaster,ExamSetUp examSetUp)
			throws BiffException, IOException {
		LOGGER.info("UploadExcelServiceImpl.uploadQuestions()");
		LOGGER.info("uload question////////////////////////////////////");
		try {

			File file = new File(path);
			LOGGER.info("findByExamIdOrderByTopicNameAsc..................");
			List<ExamTopicDetail> topicDetail = topicDetailRepository
					.findByExamIdOrderByTopicNameAsc(examId);

			Map topicsMap = new HashMap();

			for (ExamTopicDetail examTopicDetail : topicDetail) {
				String topicId=String.valueOf(examTopicDetail.getTopicId());
				topicsMap.put(topicId,examTopicDetail.getTopicName().toUpperCase());
				LOGGER.info("..................." + topicsMap);
				
			}
			/*
			 * Workbook creation
			 */

			Workbook workbook = Workbook.getWorkbook(file);
			Sheet sheet = workbook.getSheet(0);
			int rows = sheet.getRows();
			LOGGER.info(".................." + rows);
			int column = sheet.getColumns();
			LOGGER.info(column);
			if (column < 10) {
				LOGGER.info("FileNotInProperFormat"
								+ "The Excel file is not in proper format. It must have 10 columns. "
								+ "Please see the help document for the proper file structure.");

				return "The Excel file is not in proper format. It must have 10 columns."
						+ " Please see the help document for the proper file structure.";
			}

			/*
			 * a counter to maintain the number of questions that failed insert
			 * because they are incomplete
			 */

			int failedInserts = 0;

			/*
			 * getting the last question ID from the table
			 */
			LOGGER.info("currentQuestionId");
			Integer currentQuestionId = questionsRepository.maxQuestions(examId);
			LOGGER.info("currentQuestionId"+currentQuestionId);
			/*
			 * we are assuming that the questions will start in the excel sheet
			 * from the second row. remember row count starts with zero.
			 */

			List failedQuestionsList = new ArrayList();

			Set failedTopicSet = new HashSet();

			boolean fullUpload = true;
			/*
			 * Delete Question_b
			 */

			if (fullUpload == true) {
				int result = questionsRepository.deleteQuestionbankmaster_b(examId);

				currentQuestionId = 0;
			}

			for (int i = 1; i < rows; i++)

			{
				LOGGER.info("questionMaster/////////////////////for loop ");

				Cell[] cellarr = sheet.getRow(i);
				LOGGER.info("cell array length" + cellarr.length);
				if (cellarr.length > 0) {
					
					String questionDetail = cellarr[1].getContents().trim();
					LOGGER.info(questionDetail.length());
					LOGGER.info("questionDetail is " + questionDetail);

					if ((questionDetail != null)
							&& (questionDetail.length() > 1)) {
						questionMaster.setQuestionDetail(questionDetail);
						LOGGER.info("................");
						questionMaster.setOptionA(cellarr[2].getContents()
								.trim());
						questionMaster.setOptionB(cellarr[3].getContents()
								.trim());
						questionMaster.setOptionC(cellarr[4].getContents()
								.trim());
						questionMaster.setOptionD(cellarr[5].getContents()
								.trim());
						questionMaster.setOptionE(cellarr[6].getContents()
								.trim());
						String answer = cellarr[7].getContents().trim()
								.toUpperCase();
						String topicId = cellarr[8].getContents().trim()
								.toUpperCase();
						Double questionType = Double.parseDouble(cellarr[9]
								.getContents().trim());

						LOGGER.info("...questionType.." + questionType);
						LOGGER.info("...topicId.." + topicId);
						if ((answer == null) || (topicId == null)
								|| (questionType == null)) {
							failedInserts++;
							LOGGER.info("either answer or topicid or questointype is null###########");
							failedQuestionsList.add(Integer.valueOf(i));
							continue;
							/*
							 * we dont want to insert this question into the
							 * database as it is incomplete.
							 */

						}
						/*
						 * checking whether the topic is there in the database
						 */
						LOGGER.info("topicsmap....." + topicsMap+"topics....."+topicId);
						LOGGER.info("topicid....contains......" + topicsMap.containsKey(topicId));
						if (!topicsMap.containsKey(topicId)) {

							/*
							 * the topic is not there in the examtopicmaster. so
							 * dont insert this question. set a flag that this
							 * is a failed insert and continue.
							 */

							LOGGER.info("topicid.........." + topicId);

							failedInserts++;
							failedQuestionsList.add(Integer.valueOf(i));
							failedTopicSet.add(topicId);
							LOGGER.info("either topic is wrong %%%%%%%%%%%%%%%");
							continue;
						}

						questionMaster.setTopicId(topicId);
						String[] answers = answer.split(",");
						Double numAnswers = (double) answers.length;

						questionMaster.setAnswer(answer);
						questionMaster.setNumAnswers(numAnswers);
						currentQuestionId++;

						int qid = currentQuestionId;
						LOGGER.info("before map topicID"+topicId);
						LOGGER.info("before map topicID set "+topicsMap.get(questionMaster.getTopicId()));
						String topicName = (String) topicsMap.get(questionMaster.getTopicId());
						LOGGER.info("topicName"+topicName);
						Integer result = questionsRepository.insertQuestions(
								examId, qid, questionMaster.getTopicId(),
								questionMaster.getQuestionDetail(),
								questionMaster.getOptionA(),
								questionMaster.getOptionB(),
								questionMaster.getOptionC(),
								questionMaster.getOptionD(),
								questionMaster.getOptionE(),
								questionMaster.getAnswer(),
								questionMaster.getNumAnswers(), questionType);

						LOGGER.info("question added successfully......................"
										+ result);
						examSetUp.setExamId(examId);
						examSetUp.setSetUpType("Moderate");
						examSetUp.setSetUpDetails("Questions have been uploaded."
						+ "These will not get reflected in the Exam unless setup exam is done.");
						boolean result1 = masterBeanservice	.insertExamSetupDetails(examSetUp);
						
					}
				}
			}
			

			if (failedInserts > 0) {
				/*
				 * this means that there are some entries that are not inserted.
				 * so we need to alert the admin.
				 */

				String message = "These questions failed to get inserted"
						+ failedQuestionsList;
				LOGGER.info("failed topic set size is " + failedTopicSet.size());
				if (failedTopicSet.size() > 0) {
					message = message
							+ "these topic names are not there in the database"
							+ failedTopicSet;
				}
				return message;
			}

		} catch (ArrayIndexOutOfBoundsException e) {
			e.getMessage();
		}
		return "success";
	}

	/*
	 * Update User From userMaster
	 */

	public boolean updateUserHelper(UserMasterBean userMasterBean) {
		LOGGER.info("UploadExcelServiceImpl.updateUserHelper()");

		Boolean enable = true;

		int updateuser = userMasterTableRepository.updateUser(
				userMasterBean.getUserid(), userMasterBean.getPassword(),
				userMasterBean.getFirstName(), userMasterBean.getLastName(),
				userMasterBean.getPeopleID(), userMasterBean.getMailID(),
				userMasterBean.getDept(), enable,
				userMasterBean.getUserFunction());

		LOGGER.info("update successfully........" + updateuser);
		return true;

	}

	/*
	 * Add User usermaster
	 */

	@Override
	public boolean addUserHelper(UserMasterBean userMasterBean,
			UserAdminRelationship userAdminRelationship, String adminId) {
		LOGGER.info("UploadExcelServiceImpl.addUserHelper()");

		LOGGER.info("usermasterbean in adduserhelper()---------------------"+userMasterBean);
		
		String userId = userMasterBean.getUserid();
		LOGGER.info("userid............." + userId);
		String password = userMasterBean.getPassword();
		String firstName = userMasterBean.getFirstName();
		String lastName = userMasterBean.getFirstName();
		String peopleID = userId;
		String mailID = userMasterBean.getMailID();
		String dept = userMasterBean.getDept();
		Boolean enable = true;
		String userFunction = userMasterBean.getUserFunction();

		Integer adduser = userMasterTableRepository.addUser(userId, password,
				firstName, lastName, peopleID, mailID, dept, enable,
				userFunction);
		LOGGER.info(adduser);
		userAdminRelationship.setAdminId(adminId);
		userAdminRelationship.setUserId(userId);
		UserAdminRelationship addUserAdmin = userAdminRelationshipRepository
				.save(userAdminRelationship);
		LOGGER.info(adduser);
		LOGGER.info("userAdmin added successfully" + addUserAdmin);
		return true;
	}

	/*
	 * List of User Upload in current session
	 */
	@Override
	public List<UserMasterTable> listOfUsersUpload(ArrayList uploadUser,
			String adminId) {
		LOGGER.info("listOfUsersUpload-------------");

		List<UserMasterTable> masters = userMasterTableRepository
				.findByUserIdIn(uploadUser);
		LOGGER.info(masters);

		return masters;
	}

}
