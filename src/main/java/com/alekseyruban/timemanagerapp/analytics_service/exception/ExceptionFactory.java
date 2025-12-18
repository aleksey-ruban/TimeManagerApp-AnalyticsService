package com.alekseyruban.timemanagerapp.analytics_service.exception;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class ExceptionFactory {
    public ApiException userNotFountException() {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.USER_NOT_FOUND,
                "User with given email not found"
        );
    }

    public ApiException categoryNotFountException() {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.OBJECT_NOT_FOUND,
                "Category not found"
        );
    }

    public ApiException notUserContentException() {
        return new ApiException(
                HttpStatus.FORBIDDEN,
                ErrorCode.DATA_FORBIDDEN,
                "User is not author"
        );
    }

    public ApiException categoryExistsException() {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.OBJECT_EXISTS,
                "Category already exists"
        );
    }

    public ApiException badNameException() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_NAME,
                "Name must consists of words and digits"
        );
    }

    public ApiException activityExistsException() {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.OBJECT_EXISTS,
                "Activity already exists"
        );
    }

    public ApiException activityNotFoundException() {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.OBJECT_NOT_FOUND,
                "Activity not found"
        );
    }

    public ApiException badIcon() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.OBJECT_NOT_FOUND,
                "Icon doesn't exists"
        );
    }

    public ApiException badColor() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_PARAMS,
                "Color is prohibited"
        );
    }

    public ApiException oldVersion() {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.OLD_OBJECT_VERSION,
                "Version of object outdated"
        );
    }

    public ApiException activityVariationNotFound() {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.OBJECT_NOT_FOUND,
                "Activity variation not found"
        );
    }

    public ApiException variationExistsException() {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.OBJECT_EXISTS,
                "Variation already exists"
        );
    }

    public ApiException badTimeParams() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_PARAMS,
                "End time must be later than start time"
        );
    }

    public ApiException activityRecordNotFound() {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.OBJECT_NOT_FOUND,
                "Activity record not found"
        );
    }

    public ApiException sameNamesException() {
        return new ApiException(
                HttpStatus.BAD_REQUEST,
                ErrorCode.BAD_PARAMS,
                "Objects has same names"
        );
    }

    public ApiException chronometryNotFoundException() {
        return new ApiException(
                HttpStatus.NOT_FOUND,
                ErrorCode.OBJECT_NOT_FOUND,
                "Chronometry not found"
        );
    }

    public ApiException chronometryAlreadyExistsException() {
        return new ApiException(
                HttpStatus.CONFLICT,
                ErrorCode.OBJECT_EXISTS,
                "Chronometry already exists"
        );
    }

    public ApiException tooEarlyFinishChronometry() {
        return new ApiException(
                HttpStatus.TOO_EARLY,
                ErrorCode.TOO_EARLY,
                "Too early to finish chronometry"
        );
    }
}
