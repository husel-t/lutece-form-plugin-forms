/*
 * Copyright (c) 2002-2019, Mairie de Paris
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 *  1. Redistributions of source code must retain the above copyright notice
 *     and the following disclaimer.
 *
 *  2. Redistributions in binary form must reproduce the above copyright notice
 *     and the following disclaimer in the documentation and/or other materials
 *     provided with the distribution.
 *
 *  3. Neither the name of 'Mairie de Paris' nor 'Lutece' nor the names of its
 *     contributors may be used to endorse or promote products derived from
 *     this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDERS OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 *
 * License 1.0
 */
package fr.paris.lutece.plugins.forms.export.csv;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

import fr.paris.lutece.plugins.filegenerator.service.IFileGenerator;
import fr.paris.lutece.plugins.forms.business.FormResponse;
import fr.paris.lutece.plugins.forms.business.FormResponseHome;
import fr.paris.lutece.plugins.forms.business.form.FormResponseItem;
import fr.paris.lutece.plugins.forms.business.form.FormResponseItemSortConfig;
import fr.paris.lutece.plugins.forms.business.form.column.IFormColumn;
import fr.paris.lutece.plugins.forms.business.form.filter.FormFilter;
import fr.paris.lutece.plugins.forms.business.form.panel.FormPanel;
import fr.paris.lutece.plugins.forms.service.MultiviewFormService;
import fr.paris.lutece.portal.service.util.AppPropertiesService;
import fr.paris.lutece.util.file.FileUtil;

/**
 * FileGeneratro form Csv Export.
 */
public class CSVFileGenerator implements IFileGenerator
{
    private static final String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final boolean ZIP_EXPORT = Boolean.parseBoolean( AppPropertiesService.getProperty( "forms.export.csv.zip", "false" ) );
    private static final int FLUSH_SIZE = 1000;
    public static final String UTF8_BOM = "\uFEFF";

    private final FormPanel _formPanel;
    private final List<IFormColumn> _listFormColumn;
    private final List<FormFilter> _listFormFilter;
    private final FormResponseItemSortConfig _sortConfig;
    private final String _fileName;
    private final String _fileDescription;

    /**
     * Constructor
     * 
     * @param _listFormResponseItems
     */
    public CSVFileGenerator( String formName, FormPanel formPanel, List<IFormColumn> listFormColumn, List<FormFilter> listFormFilter,
            FormResponseItemSortConfig sortConfig, String fileDescription )
    {
        super( );
        _fileName = FileUtil.normalizeFileName( formName );
        _formPanel = formPanel;
        _listFormColumn = new ArrayList<>( listFormColumn );
        _listFormFilter = new ArrayList<>( listFormFilter );
        _sortConfig = sortConfig;
        _fileDescription = fileDescription;
    }

    @Override
    public Path generateFile( ) throws IOException
    {
        Path csvFile = Paths.get( TMP_DIR, _fileName + FileUtil.EXTENSION_CSV );
        writeExportFile( csvFile );
        return csvFile;
    }

    @Override
    public String getFileName( )
    {
        return _fileName + ( isZippable( ) ? FileUtil.EXTENSION_ZIP : FileUtil.EXTENSION_CSV );
    }

    @Override
    public String getMimeType( )
    {
        return isZippable( ) ? FileUtil.CONSTANT_MIME_TYPE_ZIP : FileUtil.CONSTANT_MIME_TYPE_CSV;
    }
    
    @Override
    public boolean isZippable( )
    {
        return ZIP_EXPORT;
    }

    private void writeExportFile( Path tempFile ) throws IOException
    {
        FormResponseCsvExport formResponseExport = new FormResponseCsvExport( );

        boolean first = true;
        int count = 0;
        
        try ( BufferedWriter bos = Files.newBufferedWriter( tempFile, StandardCharsets.UTF_8 ) )
        {
            bos.write( UTF8_BOM );
            List<FormResponseItem> listFormResponseItems = MultiviewFormService.getInstance( ).searchAllListFormResponseItem( _formPanel, _listFormColumn, _listFormFilter, _sortConfig );
            for ( FormResponseItem formResponseItem : listFormResponseItems )
            {
                count++;
                FormResponse formResponse = FormResponseHome
                        .findByPrimaryKeyForIndex( formResponseItem.getIdFormResponse( ) );
                if ( first )
                {
                    bos.write( formResponseExport.buildCsvColumnToExport( formResponse ) );
                    bos.newLine( );
                    first = false;
                }
                bos.write( formResponseExport.buildCsvDataToExport( formResponse ) );
                bos.newLine( );
                if ( count % FLUSH_SIZE == 0 )
                {
                    bos.flush( );
                }
            }
            bos.flush( );
        }
    }

    @Override
    public String getDescription( )
    {
        return _fileDescription;
    }
}