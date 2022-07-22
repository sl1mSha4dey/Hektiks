
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<% String partPath = (String) request.getAttribute("part"); %>

<div class="breadcrumb text">
    <ul class="user-links">
        <li class="user-link <%= partPath != null && partPath.contains("utenti") ? "active" : "" %>"><a
                href="<%= request.getContextPath() %>/admin?part=utenti">
            Utenti</a>
        </li>
        <li class="user-link <%= partPath != null && partPath.contains("prodotti") ? "active" : "" %>"><a
                href="<%= request.getContextPath() %>/admin?part=prodotti">Giochi</a></li>
        <li class="user-link <%= partPath != null && partPath.contains("giftcards") ? "active" : "" %>"><a
                href="<%= request.getContextPath() %>/admin?part=giftcards">GiftCards</a></li>
    </ul>
</div>
<jsp:include page="<%= partPath %>"/>