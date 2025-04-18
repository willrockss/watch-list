package io.kluev.watchlist.infra.googlesheet.clientimpl;

public sealed class GoogleSheetCommand permits CutInsertWithUpdateRowCommand, FindRowByValues, DeleteRow {
}
