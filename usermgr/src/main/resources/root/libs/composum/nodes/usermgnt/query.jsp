<%@page session="false" pageEncoding="utf-8"%>
<%@taglib prefix="sling" uri="http://sling.apache.org/taglibs/sling/1.2"%>
<%@taglib prefix="cpn" uri="http://sling.composum.com/cpnl/1.0"%>
<%@taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core"%>
<sling:defineObjects />
<div class="query-panel">
    <div class="query-head">
        <div class="query-actions action-bar" role="toolbar">
            <cpn:form role="search" action="/bin/cpm/nodes/node.query.html" method="GET">
                <div class="input-group">
                    <input type="text" class="form-control" placeholder="Search">
                    <span class="exec glyphicon-triangle-right glyphicon input-group-addon btn btn-default" title="Execute query..."></span>
                </div>
            </cpn:form>
        </div>
    </div>
    <div class="query-result">
        <table class="table table-striped table-hover table-condensed">
            <tbody>
            </tbody>
        </table>
    </div>
</div>
