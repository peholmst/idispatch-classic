package net.pkhapps.idispatch.server.ui.management;

import com.vaadin.flow.data.binder.Result;
import com.vaadin.flow.data.binder.ValueContext;
import com.vaadin.flow.data.converter.Converter;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

class PhoneNumbersToStringConverter implements Converter<String, Set<String>> {

    @Override
    public Result<Set<String>> convertToModel(String s, ValueContext valueContext) {
        if (s == null || s.isEmpty()) {
            return Result.ok(Collections.emptySet());
        } else {
            final Set<String> destination = new HashSet<>();
            var sb = new StringBuilder();
            for (char c : s.toCharArray()) {
                if (c == '+' || Character.isDigit(c)) {
                    sb.append(c);
                } else if (c == '\n' || c == ',') {
                    destination.add(sb.toString());
                    sb = new StringBuilder();
                }
            }
            destination.add(sb.toString());
            return Result.ok(destination);
        }
    }

    @Override
    public String convertToPresentation(Set<String> set, ValueContext valueContext) {
        return set == null ? null : String.join("\n", set);
    }
}
