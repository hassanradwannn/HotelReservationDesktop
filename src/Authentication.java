public class Authentication {
    public static User login(String username, String password, boolean isGuest)
            throws InvalidCredentialsException {
        User user = Database.findUser(username);
        if (user != null) {
            boolean matchtype = isGuest ? (user instanceof Guest) : (user instanceof Staff);
            if (matchtype) {
                if (user.getPassword().equals(password)) {
                    return user;
                }
                throw new InvalidPasswordException();
            }

        }
        throw new UserNotFoundException();
    }

    public static void register(User user) throws InvalidCredentialsException {
        if (Database.findUser(user.getUsername()) != null) {
            throw new UsernameAlreadyTakenException();
        }
        validatePasswordStrength(user.getPassword());
        Database.addUser(user);
    }

    private static void validatePasswordStrength(String password) throws WeakPasswordException {
        String regex = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z]).{8,}$";
        if (password == null || !password.matches(regex)) {
            throw new WeakPasswordException();
        }
    }
}
