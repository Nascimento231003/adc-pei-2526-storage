package pt.unl.fct.di.adc.firstwebapp.model;

public final class ApiResponse {
        public String status;
        public Object data;

        public ApiResponse() {}

        public ApiResponse(String status, Object data) {
            this.status = status;
            this.data = data;
        }

        public static ApiResponse success(Object data) { 
            return new ApiResponse ("success", data);
        }

        public static ApiResponse error(ErrorCode code) {
            return new ApiResponse(code.code(), code.defaultMessage());
        }
}

