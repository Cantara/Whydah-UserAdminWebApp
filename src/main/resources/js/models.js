/**
 * Created by huy on 6/29/2016.
 */
'use strict';

/* Model classes */
UseradminApp.factory('Application', [function () {

    function getUUID() {
        var d = new Date().getTime();
        var uuid = 'xxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/[x]/g, function (c) {
            var r = (d + Math.random() * 16) % 16 | 0;
            d = Math.floor(d / 16);
            return (c == 'x' ? r : (r & 0x3 | 0x8)).toString(16);
        });
        return uuid;
    }

    function Application(args) {

        if (typeof args != 'undefined') {

            this.id = args.id;
            this.name = args.name;
            this.company = args.company;
            this.tags = args.tags;
            this.defaultOrganizationName = args.defaultOrganizationName;
            this.defaultRoleName = args.defaultRoleName;
            this.description = args.description;
            this.applicationUrl = args.applicationUrl;
            this.logoUrl = args.logoUrl;
            this.tagList = [];
            this.applicationLog = [];

            if (typeof args.acl != 'undefined') {
                this.acl = args.acl;
            } else {
                this.acl = [];
            }

            // Fix 1: Capture 'this' reference for use inside forEach callback
            var self = this;

            angular.forEach(args.acl, function (item, index) {
                // Fix 2: Added null/undefined check for item itself
                if (!item) return;

                if (item.accessRights == null || typeof item.accessRights == 'undefined') {
                    item.accessRights = [];
                }

                // Fix 3: Use 'self' instead of 'this' inside the callback
                if (item.accessRights.includes('SSO_REDIRECT')) {
                    self.app_sso_redirect = item.applicationACLPath;
                } else if (item.accessRights.includes('OAUTH2_REDIRECT')) {
                    self.app_oauth2_redirect = item.applicationACLPath;
                }
            });

            if (!this.app_sso_redirect) {
                this.app_sso_redirect = args.applicationUrl;
            }

            this.organizationNames = args.organizationNames;
            this.roles = args.roles;

            // Fix 4: Added null/undefined check for security before accessing its properties
            this.security = args.security || {};

            this.isNew = false;

            // Fix 5: Default maxSessionTimeoutSeconds to 0 if undefined
            var ms = (this.security.maxSessionTimeoutSeconds) ? this.security.maxSessionTimeoutSeconds : 0;

            var months, days, hours, mins, secs;
            secs = ms;
            mins = Math.floor(secs / 60);
            secs = secs % 60;
            hours = Math.floor(mins / 60);
            mins = mins % 60;
            days = Math.floor(hours / 24);
            hours = hours % 24;
            months = Math.floor(days / 30);
            days = days % 30;

            if (months > 0) {
                this.timeout_number = months;
                this.timeout_unit = "MONTH(S)";
            } else if (days > 0) {
                this.timeout_number = days;
                this.timeout_unit = "DAY(S)";
            } else if (hours > 0) {
                this.timeout_number = hours;
                this.timeout_unit = "HOUR(S)";
            } else if (mins > 0) {
                this.timeout_number = mins;
                this.timeout_unit = "MINUTE(S)";
            } else if (secs > 0) {
                this.timeout_number = secs;
                this.timeout_unit = "SECOND(S)";
            } else {
                // Fix 6: Added default timeout values if all time units are 0
                this.timeout_number = 6;
                this.timeout_unit = "MONTH(S)";
            }

        } else {
            this.id = getUUID();
            this.security = {};
            this.security.secret = getUUID();
            this.organizationNames = [];
            this.roles = [];
            this.acl = [];
            this.timeout_number = 6; // 6 months
            this.timeout_unit = 'MONTH(S)';
            this.isNew = true;
            this.applicationLog = [];
            this.app_sso_redirect = '';
            this.app_oauth2_redirect = '';
            this.tagList = [];
        }
    }

    Application.prototype.isRedirectACL = function (aclPath) {
        // Fix 7: Added null/undefined check for aclPath and accessRights
        if (!aclPath || !aclPath.accessRights) return false;
        return aclPath.accessRights != null &&
            (aclPath.accessRights.includes('SSO_REDIRECT') ||
                aclPath.accessRights.includes('OAUTH2_REDIRECT'));
    };

    Application.prototype.display = function () {
        var copy = angular.copy(this);
        delete copy.timeout_unit;
        delete copy.timeout_number;
        delete copy.isNew;
        delete copy.app_oauth2_redirect;
        delete copy.app_sso_redirect;

        return JSON.stringify(copy, null, 2);
    };

    return Application;
}]);