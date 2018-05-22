/**
 * Created by huy on 6/29/2016.
 */
'use strict';

/* Model classes */
UseradminApp.factory('Application', [function () {

        function getUUID(){
            var d = new Date().getTime();
            var uuid = 'xxxxxxxxxxxxxxxxxxxxxxxxx'.replace(/[x]/g, function(c) {
                var r = (d + Math.random()*16)%16 | 0;
                d = Math.floor(d/16);
                return (c=='x' ? r : (r&0x3|0x8)).toString(16);
            });
            return uuid;
        }


        function Application(args) {


            if(typeof args!='undefined') {
              
                this.id = args.id;
                this.name = args.name;
                this.company = args.company;
                this.tags = args.tags;
                this.defaultOrganizationName = args.defaultOrganizationName;
                this.defaultRoleName = args.defaultRoleName;
                this.description = args.description;
                this.applicationUrl = args.applicationUrl;
                this.logoUrl = args.logoUrl;
                this.tagList=[];
                this.applicationLog=[];
             
                if(typeof args.acl !='undefined'){
                    this.acl = args.acl;
                } else {
                    this.acl = [];
                }
               
                var that = this;
                angular.forEach(args.acl, function(item, index){
                	if(item.accessRights==null || typeof item.accessRights =='undefined'){
                		item.accessRights=[];
                	}
                    if(item.accessRights.includes('SSO_REDIRECT')){
                    	that.app_sso_redirect = item.applicationACLPath;
                    } else if(item.accessRights.includes('OAUTH2_REDIRECT')){
                    	that.app_oauth2_redirect = item.applicationACLPath;
                    }
                });
               
                this.organizationNames = args.organizationNames;
                this.roles = args.roles;
                this.security = args.security;
                this.isNew = false;
                
                var ms = this.security.maxSessionTimeoutSeconds;
                var months, days, hours, mins, secs;
                secs = Math.floor(ms / 1000);
                mins = Math.floor(secs / 60);
                secs = secs % 60;
                hours = Math.floor(mins / 60);
                mins = mins % 60;
                days = Math.floor(hours / 24);
                hours = hours % 24;
                months = Math.floor(days/30);
                days = days % 30;


                if(months>0){
                	this.timeout_number = months;
                	this.timeout_unit = "MONTH(S)";
                } else if(days >0){
                	this.timeout_number = days;
                	this.timeout_unit = "DAY(S)";
                } else if(hours >0){
                	this.timeout_number = hours;
                	this.timeout_unit = "HOUR(S)";
                } else if(mins >0){
                	this.timeout_number = mins;
                	this.timeout_unit = "MINUTE(S)";
                } else if(secs >0){
                	this.timeout_number = secs;
                	this.timeout_unit = "SECOND(S)";
                }

                
            } else {
            	this.id= getUUID();
            	this.security = {};
            	this.security.secret= getUUID();
            	this.organizationNames=[];
            	this.roles=[];
            	this.acl=[];
            	this.timeout_number=6; //6 months
            	this.timeout_unit='MONTH(S)';
                this.isNew = true;
                this.applicationLog =[];
                this.app_sso_redirect = '';
                this.app_oauth2_redirect ='';
                this.tagList=[];
                this.applicationLog=[];
            }


        }
        
        Application.prototype.isRedirectACL = function (aclPath) {
        	return aclPath.accessRights !=null && (aclPath.accessRights.includes('SSO_REDIRECT') || aclPath.accessRights.includes('OAUTH2_REDIRECT'));
        }

        Application.prototype.display = function () {
        	var copy = angular.copy(this);
        	delete copy.timeout_unit;
        	delete copy.timeout_number;
        	delete copy.isNew;
        	delete copy.app_oauth2_redirect;
        	delete copy.app_sso_redirect;
        	
            return  JSON.stringify(copy, null, 2);
        }


        return Application;
    }]);

