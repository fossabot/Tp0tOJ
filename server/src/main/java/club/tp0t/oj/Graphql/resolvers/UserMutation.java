package club.tp0t.oj.Graphql.resolvers;

import club.tp0t.oj.Entity.ReplicaAlloc;
import club.tp0t.oj.Entity.User;
import club.tp0t.oj.Graphql.types.*;
import club.tp0t.oj.Service.*;
import club.tp0t.oj.Util.CheckHelper;
import com.coxautodev.graphql.tools.GraphQLMutationResolver;
import graphql.schema.DataFetchingEnvironment;
import graphql.servlet.context.DefaultGraphQLServletContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.regex.Pattern;

@Component
public class UserMutation implements GraphQLMutationResolver {
    @Autowired
    private BulletinService bulletinService;
    @Autowired
    private ChallengeService challengeService;
    @Autowired
    private FlagService flagService;
    @Autowired
    private ReplicaService replicaService;
    @Autowired
    private ReplicaAllocService replicaAllocService;
    @Autowired
    private SubmitService submitService;
    @Autowired
    private UserService userService;

    // user register
    public RegisterResult register(RegisterInput registerInput, DataFetchingEnvironment environment) {

        // get session from context
        DefaultGraphQLServletContext context = environment.getContext();
        HttpSession session = context.getHttpServletRequest().getSession();

        // already login
        if ((session.getAttribute("isLogin") != null && (boolean) session.getAttribute("isLogin"))) {
            return new RegisterResult("already login cannot register");
        }

        String name = registerInput.getName();
        String stuNumber = registerInput.getStuNumber();
        String password = registerInput.getPassword();
        String department = registerInput.getDepartment();
        String qq = registerInput.getQq();
        String mail = registerInput.getMail();
        String grade = registerInput.getGrade();

//        // not empty
//        if(name==null ||
//                stuNumber==null ||
//                password==null ||
//                department==null ||
//                qq==null ||
//                mail==null ||
//                grade==null) {
//            return new RegisterResult("not empty error");
//        }

//        name = name.replaceAll("\\s", "");
//        stuNumber = stuNumber.replaceAll("\\s", "");
//        qq = qq.replaceAll("\\s", "");
//        mail = mail.replaceAll("\\s", "");

        if (!registerInput.checkPass()) return new RegisterResult("invalid information");

        // TODO: validate you are a student
        // validate email
//        String EMAIL_PATTERN = "^[_A-Za-z0-9-+]+(.[_A-Za-z0-9-]+)*@" +
//                "[A-Za-z0-9-]+(.[A-Za-z0-9]+)*(.[A-Za-z]{2,})$";
//        Pattern pattern = Pattern.compile(EMAIL_PATTERN);
        if (!CheckHelper.MAIL_PATTERN.matcher(registerInput.getMail()).matches()) {
            return new RegisterResult("invalid mail");
        }

        // check duplicated user
        if (userService.checkStuNumberExistence(stuNumber)) {
            return new RegisterResult("stuNumber already in use");
        }
        if (userService.checkQqExistence(qq)) {
            return new RegisterResult("qq already in use");
        }
        if (userService.checkMailExistence(mail)) {
            return new RegisterResult("mail already in use");
        }

        // register user
        // if succeeded
        if (userService.register(name, stuNumber, password, department, qq, mail, grade)) {
            return new RegisterResult("");
        }

        // if failed
        return new RegisterResult("failed");
    }

    // user password reset
    // currently disabled
    /*
    public ResetResult reset(ResetInput input) {

        // validate user info

        // reset password

        // if succeeded
        return new ResetResult("success");

    }
    */

    // user login
    public LoginResult login(LoginInput input, DataFetchingEnvironment environment) {

        // get session from context
        DefaultGraphQLServletContext context = environment.getContext();
        HttpSession session = context.getHttpServletRequest().getSession();

        // already login
        //if((session.getAttribute("isLogin") != null && (boolean)session.getAttribute("isLogin"))) {
        //    return new LoginResult("already login");
        //}

        if (!input.checkPass()) return new LoginResult("not empty error");
        // login check
        String stuNumber = input.getStuNumber();
        String password = input.getPassword();
        // not empty
//        if (stuNumber == null) return new LoginResult("not empty error");
//        stuNumber = stuNumber.replaceAll("\\s", "");
//        if (stuNumber.equals("")) return new LoginResult("not empty error");

        // user not exists
        if (!userService.checkStuNumberExistence(stuNumber)) {
            return new LoginResult("failed");
        }

        // user password check succeeded
        if (userService.login(stuNumber, password)) {
            session.setAttribute("isLogin", true);
            session.setAttribute("userId", userService.getIdByStuNumber(stuNumber));
            // admin
            if (userService.adminCheckByStuNumber(stuNumber)) {
                session.setAttribute("isAdmin", true);
            } else session.setAttribute("isAdmin", false);
            // team
            if (userService.teamCheckByStuNumber(stuNumber)) {
                session.setAttribute("isTeam", true);
            } else session.setAttribute("isTeam", false);

            return new LoginResult("", Long.toString(userService.getIdByStuNumber(stuNumber)),
                    userService.getRoleByStuNumber(stuNumber));
        }
        // user password check failed
        else {
            return new LoginResult("failed");
        }
    }

    // user logout
    public LogoutResult logout(DataFetchingEnvironment environment) {

        // get session from context
        DefaultGraphQLServletContext context = environment.getContext();
        HttpSession session = context.getHttpServletRequest().getSession();

        if ((session.getAttribute("isLogin") == null || !(boolean) session.getAttribute("isLogin"))) {
            return new LogoutResult("not login yet");
        }

        session.setAttribute("isLogin", false);
        return new LogoutResult("");
    }

    // submit flag
    public SubmitResult submit(SubmitInput input, DataFetchingEnvironment environment) {
        // get session from context
        DefaultGraphQLServletContext context = environment.getContext();
        HttpSession session = context.getHttpServletRequest().getSession();

        // not login yet
        if (session.getAttribute("isLogin") == null || !(boolean) session.getAttribute("isLogin")) {
            return new SubmitResult("forbidden");
        }

        // not empty
//        if (input.getChallengeId() == null || input.getFlag() == null) {
//            return new SubmitResult("not empty error");
//        }
        if (!input.checkPass()) return new SubmitResult("not empty error");

        // check flag
        long challengeId = Long.parseLong(input.getChallengeId());
        long userId = (long) session.getAttribute("userId");
        //String flag = flagService.getFlagByUserIdAndChallengeId(userId, challengeId);
        String flag = replicaService.getFlagByUserIdAndChallengeId(userId, challengeId);
        String submitFlag = input.getFlag();

        // correct flag
        if (submitFlag.equals(flag)) {
            // duplicate submit
            User user = userService.getUserById(userId);
            if (submitService.checkDuplicateSubmit(user, challengeId)) {
                return new SubmitResult("duplicate submit");
            }

            // Transactional from here (for dynamic score)
            // add user score
            // TODO: get current points of challenge
            long currentPoints;
            try {
                currentPoints = Long.parseLong(challengeService.getConfiguration(challengeService.getChallengeByChallengeId(challengeId)).getScoreEx().getBase_score());
            } catch (NumberFormatException e) {
                return new SubmitResult("unknown error");
            }
            userService.addScore(userId, currentPoints);

            // TODO: calculate new points
            int solvedCount = submitService.updateSolvedCountByChallengeId(challengeId, user);
            long newPoints = currentPoints;

            // update score of user who has solved this challenge
            if (currentPoints != newPoints) {
                userService.updateScore(challengeId, currentPoints, newPoints);
            }
            // Transactional end here

            // whether first three solvers
            int mark = 0;
            if (solvedCount <= 3) {
                mark = solvedCount;
            }

            // save into submit table
            submitService.submit(userService.getUserById(userId), submitFlag, true, mark);

            return new SubmitResult("correct");
        }
        // incorrect flag
        else {
            // save into submit table
            submitService.submit(userService.getUserById(userId), submitFlag, false, 0);
            return new SubmitResult("incorrect");
        }

    }

    // admin update user info
    public UserInfoUpdateResult userInfoUpdate(UserInfoUpdateInput input, DataFetchingEnvironment environment) {
        // get session from context
        DefaultGraphQLServletContext context = environment.getContext();
        HttpSession session = context.getHttpServletRequest().getSession();

        // login & admin check
        if (session.getAttribute("isLogin") == null ||
                !((boolean) session.getAttribute("isLogin")) ||
                !(boolean) session.getAttribute("isAdmin")) {
            return new UserInfoUpdateResult("forbidden");
        }

        if (!input.checkPass()) return new UserInfoUpdateResult("");

        // cannot change one's own role
        long userId = (long) session.getAttribute("userId");
        if (userService.adminCheckByUserId(userId) &&
                Long.parseLong(input.getUserId()) == userId && !input.getRole().equals("admin")) {
            return new UserInfoUpdateResult("downgrade not permitted");
        }

        userService.updateUserInfo(input.getUserId(),
                input.getName(),
                input.getRole(),
                input.getDepartment(),
                input.getGrade(),
                input.getProtectedTime(),
                input.getQq(),
                input.getMail(),
                input.getState());

        return new UserInfoUpdateResult("");

    }

}
